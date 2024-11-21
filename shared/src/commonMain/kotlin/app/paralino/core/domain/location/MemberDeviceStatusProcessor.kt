package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MemberDeviceStatusProcessor(
    private val local: UserGroupLocalDataSource,
    private val remote: LocationRemoteSource,
    private val payloadProvider: LocationPayloadProvider,
    private val sharingManager: LocationSharingManager,
    private val oneTimeLocationProvider: IOneTimeLocationProvider,
    private val appPrefs: AppPrefs
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
                data = oneTimeLocationProvider.getLastLocation(IOneTimeLocationProvider.RequestType.CacheOnly),
                contract = sharingManager.currentSharingContractForGroup(groupId)
            ).payload

            if (payload != null) {
                logg.v("device_status","Update device status in group $groupId with location (${payload.locationData != null})")
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
                    pushDeviceStatus(groupId = it.groupId)
                } catch (e: Exception) {
                    logg.w("Error while resetting device status", e)
                }
                delay(300)
            }
        }
    }

    suspend fun deleteOldLocations() {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "old_location_reset",
                timeMillis = 4.hours.inWholeMilliseconds
            )
        ) {
            val memberModels = local.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation }
                .filter {
                    val sharingActive = sharingManager.isSharingActiveForGroup(it.groupId)
                    val canClear = it.canAutomaticallyClearLocation
                    !sharingActive && canClear
                }

            logg.d("Clearing old locations for ${memberModels.size} groups")
            memberModels.forEach {
                try {
                    remote.clearLocation(groupId = it.groupId, locationId = it.id)
                } catch (e: Exception) {
                    logg.w("Error while clearing old location", e)
                }
                delay(300)
            }
        }
    }
}