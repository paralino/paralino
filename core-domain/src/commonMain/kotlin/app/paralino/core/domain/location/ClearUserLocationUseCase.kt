package app.paralino.core.domain.location

import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.hours

class ClearUserLocationUseCase(
    private val groupLocal: GetMemberWithLocationsUseCase,
    private val local: LocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val personalDevicesLocal: PersonalDevicesLocalDataSource,
    private val locationSharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs
) : BaseRepository() {

    fun clearLocation(
        groupId: GroupOrSpaceId,
        locationId: String
    ): Flow<Unit> {
        return getDataOnce(
            remote = { remote.clearLocation(groupId = groupId, locationId = locationId) },
            localUpdate = { local.clearMemberLocationData(groupOrSpaceId = groupId, locationId = locationId) }
        )
    }

    suspend fun deleteOldLocations() {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "old_location_reset",
                timeMillis = 4.hours.inWholeMilliseconds
            )
        ) {
            val memberModels = groupLocal.getMemberModelsWithLocationForUser(appPrefs.userId)
                .mapNotNull { it.currentDeviceLocation }
                .let {
                    val personalSpaceLocation = personalDevicesLocal.getUserDeviceByDeviceIdOnce(
                        deviceId = appPrefs.deviceId
                    )?.location
                    if (personalSpaceLocation != null) {
                        it.plus(personalSpaceLocation)
                    } else {
                        it
                    }
                }
                .filter {
                    val sharingActive = locationSharingManager.isSharingActiveForGroup(it.groupOrSpaceId)
                    val canClear = it.canAutomaticallyClearLocation
                    !sharingActive && canClear
                }

            logg.d("Clearing old locations for ${memberModels.size} groups")
            memberModels.forEach {
                try {
                    remote.clearLocation(groupId = it.groupOrSpaceId, locationId = it.id)
                } catch (e: Exception) {
                    logg.w("Error while clearing old location", e)
                }
                delay(300)
            }
        }
    }
}