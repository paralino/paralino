package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

class RevokeDeviceActiveStatusUseCase(
    private val local: GetMemberWithLocationsUseCase,
    private val sharingManager: LocationSharingManager,
    private val memberDeviceStatusProcessor: MemberDeviceStatusProcessor,
    private val appPrefs: AppPrefs
) : BaseRepository() {
    suspend fun revokeActiveStatus() {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "device_status_reset",
                timeMillis = 10.minutes.inWholeMilliseconds
            )
        ) {
            val memberModels = local.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation }
                .filter {
                    val sharingActive = sharingManager.isSharingActiveForGroup(it.groupId)
                    val statusActive =
                        it.locationAwareStatusType.let { it != null && it != MemberDeviceStatusType.NOT_SHARING }
                    !sharingActive && statusActive
                }

            logg.d("Revoking device status for ${memberModels.size} groups")
            memberModels.forEach {
                try {
                    memberDeviceStatusProcessor.pushDeviceStatus(groupId = it.groupId)
                } catch (e: Exception) {
                    logg.w("Error while resetting device status", e)
                }
                delay(300)
            }
        }
    }
}