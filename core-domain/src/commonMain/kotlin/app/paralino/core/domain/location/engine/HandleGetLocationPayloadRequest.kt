package app.paralino.core.domain.location.engine

import app.paralino.core.domain.location.IReRegisterDeviceUseCase
import app.paralino.core.domain.location.LocationPayloadProvider
import app.paralino.core.domain.location.LocationPayloadStatus
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.utils.exceptions.DeviceSafetySignatureCorrupted

class HandleGetLocationPayloadRequest(
    private val payloadProvider: LocationPayloadProvider,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val registerDeviceForSharing: IReRegisterDeviceUseCase,
    private val locationSharingManager: LocationSharingManager,
) {
    suspend fun getLocationPayload(
        groupId: GroupOrSpaceId,
        data: LocationDataModel?,
        contract: LocationSharingContract,
    ): MemberGroupId? {

        val payload = payloadProvider.getLocationPayload(
            groupId = groupId,
            data = data,
            contract = contract
        )
        return when (payload) {
            is LocationPayloadStatus.Success -> payload.payload

            LocationPayloadStatus.MissingLocationDocument -> {
                val result = try {
                    registerDeviceForSharing.tryRegisterDeviceAgain(
                        groupId = groupId,
                        data = data,
                        contract = contract
                    )
                } catch (e: Exception) {
                    if (e is DeviceSafetySignatureCorrupted) {
                        logg.i(
                            "location_service",
                            "Location device safety signature is corrupted, abort sharing"
                        )
                        locationSharingManager.stopSharing(groupId = groupId, deleteContract = false)
                        placeActivityRepo.clearForGroup(groupId = groupId)
                        return null
                    } else {
                        false
                    }
                }

                if (result) {
                    // retry once again
                    getLocationPayload(
                        groupId = groupId,
                        data = data,
                        contract = contract
                    )
                } else {
                    null
                }
            }

            LocationPayloadStatus.MissingData -> {
                logg.i(
                    "location_service",
                    "Location device can not be established for current device, abort sharing"
                )
                locationSharingManager.stopSharing(groupId = groupId, deleteContract = false)
                placeActivityRepo.clearForGroup(groupId = groupId)
                null
            }

            LocationPayloadStatus.Processing -> null
        }
    }
}