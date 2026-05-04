package app.paralino.core.repo.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.Resource
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import app.paralino.core.models.raw.UserDevicesWithLocationsAndPlacesRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.places.PlaceActivityDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserPersonalSpaceLocalDataSource constructor(
    private val database: MainDatabaseHelper,
    private val placeMapper: GroupPlaceMapperLocal,
    private val placeActivity: PlaceActivityDataSource,
    private val userRepo: UserRepository,
    private val devicesLocal: PersonalDevicesLocalDataSource,
    private val appPrefs: AppPrefs,
    private val scopeProvider: ICoroutineScopeProvider
) {

    val personalSpaceState: StateFlow<Resource<UserPersonalSpaceModel?>> =
        getPersonalSpaceWithDevices()
            .map { Resource.Ready(it) }
            .conflate()
            .distinctUntilChanged()
            .stateIn(
                scope = scopeProvider.defaultIOScope,
                started = SharingStarted.Eagerly,
                initialValue = Resource.NotInitialized
            )

    suspend fun recreateDevicesWithPlaces(data: UserDevicesWithLocationsAndPlacesRaw) {
        devicesLocal.recreateDevices(devices = data.devices)
        recreatePersonalPlaces(places = data.places)
    }

    suspend fun recreatePersonalPlaces(places: List<GroupPlaceWithSettingsRaw>) {
        val personalPlaces = places.filter { it.place.isInPersonalSpace == true }
        database.groupRelatedDao.recreateAllPlacesWithSettings(
            groupId = appPrefs.userId,
            placeEntity = personalPlaces.map { placeMapper.remote2Local(it.place) },
            placeSettings = personalPlaces.mapNotNull { it.settings?.let { placeMapper.remote2Local(it) } }
        )
        placeActivity.recreatePlacesForGroup(
            groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
            places = personalPlaces
        )
    }

    fun getPersonalSpaceWithDevices(): Flow<UserPersonalSpaceModel?> {
        return combine(
            userRepo.localUser.map { it.data }.distinctUntilChanged(),
            devicesLocal.getAllUserDevices(),
            userRepo.userPlans,
            database.groupRelatedDao.getPlacesCountForPersonalSpace()
        ) { user, devices, plans, placesCount ->
            if (user != null) {
                UserPersonalSpaceModel(
                    user = user,
                    plans = plans,
                    devices = devices,
                    placesCount = placesCount ?: 0
                )
            } else {
                null
            }
        }
    }
} 