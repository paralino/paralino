package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.MemberEntity
import app.paralino.core.db.main.groups.MemberLocationEntity
import app.paralino.core.domain.crypto.IMemberCryptoKeyRotation
import app.paralino.core.domain.crypto.IMemberDeviceKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryPointLocationModel
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.LocationHistoryPointStatusModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.utils.extensions.roundToPrecision
import app.paralino.core.utils.extensions.safeLet

class LocationPayloadProvider(
    private val databaseHelper: MainDatabaseHelper,
    private val database: MainDatabaseHelper,
    private val appPrefs: AppPrefs,
    private val memberDeviceStatusProvider: MemberDeviceStatusProvider,
    private val memberCryptoKeyRotation: IMemberCryptoKeyRotation,
    private val deviceKeyRotation: IMemberDeviceKeyRotation,
) {

    suspend fun getLocationPayload(
        groupId: String,
        data: LocationDataModel?,
        /**
         * if there is no contract, location data is ignored
         */
        contract: LocationSharingContract?
    ): LocationPayloadStatus {
        // skip location update if key rotation is in progress
        if (
            memberCryptoKeyRotation.isProcessing(groupId) ||
            deviceKeyRotation.isProcessing(groupId)
        ) {
            return LocationPayloadStatus.Processing
        }

        val memberWithCurrentLocation = getMemberWithCurrentDeviceLocationOnce(
            groupId = groupId
        )

        val member = memberWithCurrentLocation?.first
        val location = memberWithCurrentLocation?.second
        val groupOwnerUserId = databaseHelper.groupRelatedDao.getGroupOwnerUserId(groupId = groupId)

        if (member != null && location == null) {
            logg.d("Location is null for $groupId")
            return LocationPayloadStatus.MissingLocationDocument
        } else if (member == null || location == null || groupOwnerUserId == null) {
            return LocationPayloadStatus.MissingData
        }

            val status = memberDeviceStatusProvider.getStatus(groupId = groupId, contract = contract)
            val locationData = safeLet(data, contract) { data, contract ->
                LocationDataModel(
                    timeUTC = data.timeUTC,
                    endTimeUTC = contract.endTimeUTC,
                    lat = data.lat,
                    lng = data.lng,
                    accuracy = data.accuracy,
                    speed = data.speed?.takeIf { contract.shareSpeed && it != 0f },
                    altitude = data.altitude?.takeIf { contract.shareElevation && it != 0f },
                    isMasked = false
                )
            }

            val locationHistory = safeLet(
                locationData,
                contract?.takeIf { it.saveToHistory }
            ) { data, contract ->
                if (data.timeUTC != null && status.timeUTC != null) {
                    LocationHistoryPointModel(
                        loc = LocationHistoryPointLocationModel(
                            tA = data.timeUTC,
                            tB = data.timeUTC,
                            lat = data.lat,
                            lng = data.lng,
                            // we only need partial decimal places for accuracy, speed and elevation
                            // that is already more than enough
                            acc = data.accuracy.roundToPrecision(decimalPlaces = 2),
                            spd = data.speed?.roundToPrecision(decimalPlaces = 2),
                            alt = data.altitude?.roundToPrecision(decimalPlaces = 2)
                        ),
                        status = LocationHistoryPointStatusModel(
                            tA = status.timeUTC,
                            tB = status.timeUTC,
                            stat = status.statusType?.id,
                            bat = status.batteryLevel,
                            chrg = if (status.isCharging == null) null else if (status.isCharging) 1 else 0,
                            net = status.network,
                            sig = status.signal
                        ),
                    )
                } else {
                    null
                }
            }

            return LocationPayloadStatus.Success(
                MemberGroupId(
                    locationId = location.id,
                    memberId = member.id,
                    groupId = groupId,
                    locationData = locationData,
                    deviceStatus = status,
                    locationHistory = locationHistory
                )
            )
    }

    private suspend fun getMemberWithCurrentDeviceLocationOnce(
        groupId: String
    ): Pair<MemberEntity, MemberLocationEntity?>? {
        val member = database.groupRelatedDao.getMemberOnce(
            groupId = groupId,
            userId = appPrefs.userId
        ) ?: return null

        val location = database.groupRelatedDao.getSingleLocationForMemberAndDeviceOnce(
            memberId = member.id,
            deviceId = appPrefs.deviceId
        )

        return member to location
    }
}

sealed class LocationPayloadStatus() {
    open val payload: MemberGroupId? = null

    object MissingLocationDocument : LocationPayloadStatus()
    object Processing : LocationPayloadStatus()
    object MissingData : LocationPayloadStatus()
    data class Success(override val payload: MemberGroupId) : LocationPayloadStatus()
}