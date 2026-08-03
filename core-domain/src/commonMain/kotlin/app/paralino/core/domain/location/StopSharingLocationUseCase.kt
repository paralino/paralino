package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.places.PlaceActivityDataSource

class StopSharingLocationUseCase(
    private val sharingManager: LocationSharingManager,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
) {
    suspend fun stopSharing(groupOrSpaceId: GroupOrSpaceId) {
        sharingManager.stopSharing(groupId = groupOrSpaceId, deleteContract = false)
        placeActivityRepo.clearForGroup(groupId = groupOrSpaceId)
        try {
            deviceStatusProcessor.pushDeviceStatus(groupId = groupOrSpaceId)
        } catch (e: Exception) {
            logg.w("Error while updating user status", e)
        }
    }
}