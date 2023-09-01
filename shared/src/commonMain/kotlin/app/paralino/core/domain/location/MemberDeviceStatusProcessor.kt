package app.paralino.core.domain.location

import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes

class MemberDeviceStatusProcessor(
    private val local: LocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val payloadProvider: LocationPayloadProvider,
    private val sharingManager: LocationSharingManager
) : BaseRepository() {


    suspend fun pushDeviceStatus(groupId: String) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "device_status",
                timeMillis = 1_000
            )
        ) {
            val payload = payloadProvider.getLocationPayload(
                groupId = groupId,
                data = null,
                contract = null
            ).payload

            if (payload != null) {
                logg.v("Update device status in group $groupId")
                remote.saveCurrentLocation(payload)
            }
        }
    }

    suspend fun revokeActiveStatus() {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "device_status_reset",
                timeMillis = 10.minutes.inWholeMilliseconds
            )
        ) {
            val memberModels = local.getMemberModelsWithLocationForUser(config.userId)
                .mapNotNull { it.currentDeviceLocation }
                .filter {
                    val sharingActive = sharingManager.isSharingActiveForGroup(it.groupId)
                    val statusActive =
                        it.locationAwareStatusType.let { it != null && it != MemberDeviceStatusType.NOT_SHARING }
                    !sharingActive && statusActive
                }

            logg.w("Revoking device status for ${memberModels.size} groups")
            memberModels.forEach {
                try {
                    pushDeviceStatus(it.groupId)
                } catch (e: Exception) {
                    logg.w("Error while resetting device status", e)
                }
                delay(300)
            }
        }
    }
}