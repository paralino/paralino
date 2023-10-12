package app.paralino.core.domain.location

import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
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
                LocationPayloadStatus.Success(
                    MemberGroupId(
                        locationId = locationId,
                        groupId = groupId,
                        member = member,
                        // TODO share approximate location if selected
                        locationData = safeLet(data, contract) { data, contract ->
                            LocationDataModel(
                                timeUTC = data.timeUTC,
                                endTimeUTC = contract.endTimeUTC,
                                lat = data.lat,
                                lng = data.lng,
                                accuracy = data.accuracy,
                                speed = data.speed?.takeIf { contract.shareSpeed && it != 0f },
                                altitude = data.altitude?.takeIf { contract.shareElevation && it != 0f }
                            )
                        },
                        deviceStatus = memberDeviceStatusProvider.getStatus(groupId = groupId)
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