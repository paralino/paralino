package app.paralino.core.domain.location

import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.safeLet

class LocationPayloadProvider(
    private val memberIdUseCase: UserMemberIdUseCase,
    private val memberDeviceStatusProvider: MemberDeviceStatusProvider,
    private val memberCryptoKeyRotation: MemberCryptoKeyRotation,
    private val deviceKeyRotation: MemberDeviceKeyRotation
) : BaseRepository() {


    suspend fun getLocationPayload(
        groupId: String,
        data: LocationDataModel?,
        /**
         * if there is no contract, location data is ignored
         */
        contract: LocationSharingContract?
    ): LocationPayloadStatus {
        return triggerRemoteOnly {
            // skip location update if key rotation is in progress
            if (
                memberCryptoKeyRotation.isProcessing(groupId) ||
                deviceKeyRotation.isProcessing(groupId)
            ) {
                return@triggerRemoteOnly LocationPayloadStatus.Processing
            }

            val locationId = memberIdUseCase.getCurrentLocationIdForGroup(groupId = groupId)

            if (locationId.isNullOrBlank()) {
                logg.d("Location is null for $groupId")
                return@triggerRemoteOnly LocationPayloadStatus.MissingLocationDocument
            }

            val member = memberIdUseCase.getCurrentUserMemberForGroup(groupId = groupId)

            if (locationId.isNullOrBlank() || member == null) {
                LocationPayloadStatus.MissingData
            } else if (!member.isVerified) {
                LocationPayloadStatus.UnverifiedMember
            } else {
                val status = memberDeviceStatusProvider.getStatus(groupId = groupId)
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
                    contract?.takeIf { it.locationHistoryDaysMax == null || it.locationHistoryDaysMax > 0 }
                ) { data, contract ->
                    LocationHistoryModel(
                        history = listOf(
                            LocationHistoryPointModel(
                                lat = data.lat,
                                lng = data.lng,
                                acc = data.accuracy,
                                timeUTC = data.timeUTC,
                                speed = data.speed,
                                alt = data.altitude,
                                bat = status.batteryLevel,
                                charging = status.isCharging,
                                network = status.network,
                                signal = status.signal
                            )
                        )
                    )
                }

                LocationPayloadStatus.Success(
                    MemberGroupId(
                        locationId = locationId,
                        groupId = groupId,
                        member = member,
                        locationData = locationData,
                        locationHistory = locationHistory,
                        deviceStatus = status
                    )
                )
            }
        }
    }
}

sealed class LocationPayloadStatus() {
    open val payload: MemberGroupId? = null

    object MissingLocationDocument : LocationPayloadStatus()
    object UnverifiedMember : LocationPayloadStatus()
    object Processing : LocationPayloadStatus()
    object MissingData : LocationPayloadStatus()
    data class Success(override val payload: MemberGroupId) : LocationPayloadStatus()
}