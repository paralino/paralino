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
import app.paralino.core.models.location.LocationUpdateDataFromDevice
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.utils.extensions.isLocationInaccurate
import app.paralino.core.utils.extensions.roundToPrecision
import app.paralino.core.utils.extensions.safeLet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocationPayloadProvider(
    private val database: MainDatabaseHelper,
    private val appPrefs: AppPrefs,
    private val memberDeviceStatusProvider: MemberDeviceStatusProvider,
    private val memberCryptoKeyRotation: IMemberCryptoKeyRotation,
    private val memberDeviceKeyRotation: IMemberDeviceKeyRotation,
    private val personalDeviceKeyRotation: IPersonalDeviceCryptoKeyRotation,
    private val cryptoCore: CryptoCore,
    private val approximateLocationObfuscator: ApproximateLocationObfuscator,
    private val privacyZoneObfuscator: PrivacyZoneObfuscator,
) {

    private val mutex = Mutex()

    suspend fun getLocationPayload(
        groupId: GroupOrSpaceId,
        data: LocationUpdateDataFromDevice?,
        /**
         * if there is no contract, location data is ignored
         */
        contract: LocationSharingContract?
    ): LocationPayloadStatus {
        return mutex.withLock {
            tryGetLocationPayload(
                groupId = groupId,
                data = data,
                contract = contract
            )
        }
    }

    private suspend fun tryGetLocationPayload(
        groupId: GroupOrSpaceId,
        data: LocationUpdateDataFromDevice?,
        contract: LocationSharingContract?
    ): LocationPayloadStatus {

        val result = if (groupId.isPersonalSpace) {
            personalPayloadPrecheck()
        } else {
            groupPayloadPrecheck(groupId = groupId.id)
        }

        when (result) {
            is LocationPayloadStatus.Success -> {
                val status = memberDeviceStatusProvider.getStatus(
                    groupId = groupId,
                    contract = contract
                )
                var isLikelyInaccurate = false
                val adjustedLocationData = safeLet(data, contract) { data, contract ->
                    val zoneResult = if (!groupId.isPersonalSpace) {
                        privacyZoneObfuscator.obfuscateIfInsideZone(
                            groupId = groupId,
                            trueLat = data.lat,
                            trueLng = data.lng,
                            accuracyMeters = data.adjustedHorizontalAccuracy,
                        )
                    } else {
                        null
                    }

                    val approximateRadius = contract.approximateLocationInMeters

                    if (zoneResult != null || approximateRadius != null) {
                        if (data.adjustedHorizontalAccuracy.isLocationInaccurate()) {
                            // ignore inaccurate locations since it will cause jumps
                            // and since location is already spoofed, this just pollutes obfuscators
                            return@safeLet null
                        }
                    }

                    val isInsidePrivacyZone = zoneResult != null
                    isLikelyInaccurate = !isInsidePrivacyZone &&
                            approximateRadius == null &&
                            data.isLikelyInaccurate == true
                    val adjustedCoordinates = if (approximateRadius != null) {
                        // sharing approximate
                        if (zoneResult == null || zoneResult.uploadAccuracyMeters < approximateRadius) {
                            // adjust with approximate only if its radius is bigger than current privacy zone radius
                            approximateLocationObfuscator.obfuscate(
                                groupId = groupId,
                                trueLat = data.lat,
                                trueLng = data.lng,
                                radiusMeters = approximateRadius
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    val adjustedAccuracy = if (approximateRadius != null && zoneResult != null) {
                        maxOf(
                            approximateRadius.toFloat(),
                            zoneResult.uploadAccuracyMeters.toFloat()
                        )
                    } else {
                        approximateRadius?.toFloat()
                            ?: zoneResult?.uploadAccuracyMeters?.toFloat()
                            ?: data.adjustedHorizontalAccuracy
                    }

                    val isSharingInPrivacyZone = isInsidePrivacyZone && contract.shareDeviceStatus && zoneResult.shareStatus
                    val isSharingApproximate = (approximateRadius != null || isInsidePrivacyZone) && contract.shareDeviceStatus
                    val isApproximate = isSharingApproximate || isSharingInPrivacyZone

                    LocationDataModel(
                        timeUTC = data.timeUTC,
                        endTimeUTC = contract.endTimeUTC?.takeIf { contract.shareDeviceStatus },
                        lat = (adjustedCoordinates?.location?.lat ?: zoneResult?.lat ?: data.lat)
                            .roundToPrecision(decimalPlaces = 6),
                        lng = (adjustedCoordinates?.location?.lng ?: zoneResult?.lng ?: data.lng)
                            .roundToPrecision(decimalPlaces = 6),
                        accuracy = adjustedAccuracy.roundToPrecision(decimalPlaces = 0),
                        speed = data.speed?.takeIf {
                            contract.shareSpeed && it != 0f && !isApproximate
                        },
                        altitude = data.altitude?.takeIf {
                            contract.shareElevation && it != 0f && !isApproximate
                        }?.roundToPrecision(0),
                        isMasked = false,
                        isApproximate = isApproximate,
                        isInPrivacyZone = isSharingInPrivacyZone,
                        privacyZoneMessage = if (isSharingInPrivacyZone) {
                            zoneResult.message
                        } else {
                            null
                        }
                    )
                }

                val locationHistory = safeLet(
                    adjustedLocationData,
                    contract?.takeIf { it.saveToHistory }
                ) { adjustedLocationData, contract ->
                    if (adjustedLocationData.timeUTC != null && status.timeUTC != null) {
                        LocationHistoryPointModel(
                            id = cryptoCore.randomId(length = 8, alphabet = AlphabetType.EXTENDED),
                            loc = LocationHistoryPointLocationModel(
                                tA = adjustedLocationData.timeUTC,
                                tB = null,
                                lat = adjustedLocationData.lat.roundToPrecision(decimalPlaces = 5),
                                lng = adjustedLocationData.lng.roundToPrecision(decimalPlaces = 5),
                                // we only need partial decimal places for accuracy, speed and elevation
                                // that is already more than enough
                                acc = adjustedLocationData.accuracy.roundToPrecision(decimalPlaces = -1),
                                spd = adjustedLocationData.speed?.roundToPrecision(decimalPlaces = 1),
                                alt = adjustedLocationData.altitude?.roundToPrecision(decimalPlaces = -1),
                                ina = isLikelyInaccurate.takeIf { it }
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
                        locationData = adjustedLocationData,
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