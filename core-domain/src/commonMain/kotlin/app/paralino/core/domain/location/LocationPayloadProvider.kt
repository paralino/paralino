package app.paralino.core.domain.location

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.MemberEntity
import app.paralino.core.db.main.groups.MemberLocationEntity
import app.paralino.core.domain.crypto.group.IMemberCryptoKeyRotation
import app.paralino.core.domain.crypto.group.IMemberDeviceKeyRotation
import app.paralino.core.domain.crypto.user.IPersonalDeviceCryptoKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationHistoryPointLocationModel
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.LocationHistoryPointStatusModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.utils.extensions.roundToPrecision
import app.paralino.core.utils.extensions.safeLet

class LocationPayloadProvider(
    private val database: MainDatabaseHelper,
    private val appPrefs: AppPrefs,
    private val memberDeviceStatusProvider: MemberDeviceStatusProvider,
    private val memberCryptoKeyRotation: IMemberCryptoKeyRotation,
    private val memberDeviceKeyRotation: IMemberDeviceKeyRotation,
    private val personalDeviceKeyRotation: IPersonalDeviceCryptoKeyRotation,
    private val cryptoCore: CryptoCore,
    private val approximateLocationObfuscator: ApproximateLocationObfuscator,
) {

    suspend fun getLocationPayload(
        groupId: GroupOrSpaceId,
        data: LocationDataModel?,
        /**
         * if there is no contract, location data is ignored
         */
        contract: LocationSharingContract?
    ): LocationPayloadStatus {

        val result = if (groupId.isPersonalSpace) {
            personalPayloadPrecheck()
        } else {
            groupPayloadPrecheck(groupId = groupId.id)
        }

        when (result) {
            is LocationPayloadStatus.Success -> {
                val status = memberDeviceStatusProvider.getStatus(groupId = groupId, contract = contract)
                val locationData = safeLet(data, contract) { data, contract ->
                    val approximateRadius = contract.approximateLocationInMeters
                    val isSharingApproximate = approximateRadius != null
                    val adjustedCoordinates = if (approximateRadius != null) {
                        approximateLocationObfuscator.obfuscate(
                            groupId = groupId,
                            trueLat = data.lat,
                            trueLng = data.lng,
                            radiusMeters = approximateRadius
                        )
                    } else {
                        null
                    }

                    val isApproximate = isSharingApproximate && contract.shareDeviceStatus

                    LocationDataModel(
                        timeUTC = data.timeUTC,
                        endTimeUTC = contract.endTimeUTC?.takeIf { contract.shareDeviceStatus },
                        lat = (adjustedCoordinates?.lat ?: data.lat).roundToPrecision(decimalPlaces = 6),
                        lng = (adjustedCoordinates?.lng ?: data.lng).roundToPrecision(decimalPlaces = 6),
                        accuracy = approximateRadius?.toFloat() ?: data.accuracy,
                        speed = data.speed?.takeIf {
                            contract.shareSpeed && it != 0f && !isApproximate
                        },
                        altitude = data.altitude?.takeIf {
                            contract.shareElevation && it != 0f && !isApproximate
                        },
                        isMasked = false,
                        isApproximate = isApproximate,
                    )
                }

                val locationHistory = safeLet(
                    locationData,
                    contract?.takeIf { it.saveToHistory }
                ) { data, contract ->
                    if (data.timeUTC != null && status.timeUTC != null) {
                        LocationHistoryPointModel(
                            id = cryptoCore.randomId(length = 8, alphabet = AlphabetType.EXTENDED),
                            loc = LocationHistoryPointLocationModel(
                                tA = data.timeUTC,
                                tB = null,
                                lat = data.lat.roundToPrecision(decimalPlaces = 5),
                                lng = data.lng.roundToPrecision(decimalPlaces = 5),
                                // we only need partial decimal places for accuracy, speed and elevation
                                // that is already more than enough
                                acc = data.accuracy.roundToPrecision(decimalPlaces = -1),
                                spd = data.speed?.roundToPrecision(decimalPlaces = 1),
                                alt = data.altitude?.roundToPrecision(decimalPlaces = -1)
                            ),
                            status = LocationHistoryPointStatusModel(
                                tA = status.timeUTC,
                                tB = null,
                                tz = status.tz,
                                sts = status.statusType?.id,
                                bat = status.batteryLevel,
                                chr = if (status.isCharging == null) null else if (status.isCharging) 1 else 0,
                                net = status.network,
                                sig = status.signal
                            ),
                        )
                    } else {
                        null
                    }
                }

                return result.copy(
                    payload = result.payload.copy(
                        locationData = locationData,
                        deviceStatus = status,
                        locationHistory = locationHistory
                    )
                )
            }
            else -> return result
        }
    }

    private suspend fun groupPayloadPrecheck(
        groupId: String,
    ): LocationPayloadStatus {
        // skip location update if key rotation is in progress
        if (
            memberCryptoKeyRotation.isProcessing(groupId) ||
            memberDeviceKeyRotation.isProcessing(groupId)
        ) {
            return LocationPayloadStatus.Processing
        }

        val memberWithCurrentLocation = getMemberWithCurrentDeviceLocationOnce(
            groupId = groupId
        )

        val member = memberWithCurrentLocation?.first
        val location = memberWithCurrentLocation?.second
        val groupOwnerUserId = database.groupRelatedDao.getGroupOwnerUserId(groupId = groupId)

        if (member != null && location == null) {
            logg.d("Location is null for $groupId")
            return LocationPayloadStatus.MissingLocationDocument
        } else if (member == null || location == null || groupOwnerUserId == null) {
            return LocationPayloadStatus.MissingData
        }

        return LocationPayloadStatus.Success(
            MemberGroupId(
                locationId = location.id,
                memberId = member.id,
                groupId = groupId.asGroupOrSpaceId(),
                locationData = null,
                deviceStatus = null,
                locationHistory = null
            )
        )
    }


    private suspend fun personalPayloadPrecheck(): LocationPayloadStatus {
        // skip location update if key rotation is in progress
        if (personalDeviceKeyRotation.isProcessingAny()) {
            return LocationPayloadStatus.Processing
        }

        val device = database.userDeviceDao.getUserDeviceByDeviceIdOnce(deviceId = appPrefs.deviceId)
        val locationId = device?.locationId

        if (device != null && locationId.isNullOrBlank()) {
            logg.d("Location is null for personal space")
            return LocationPayloadStatus.MissingLocationDocument
        } else if (device?.hasValidLocation != true || locationId.isNullOrBlank()) {
            return LocationPayloadStatus.MissingData
        }

        return LocationPayloadStatus.Success(
            MemberGroupId(
                locationId = locationId,
                memberId = appPrefs.userId,
                groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                locationData = null,
                deviceStatus = null,
                locationHistory = null
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