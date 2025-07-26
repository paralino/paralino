package app.paralino.core.repo.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.user.UserDeviceEntityDeviceUpdate
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.models.mappers.GroupPlaceMapperLocal
import app.paralino.core.models.mappers.UserDeviceMapper
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.models.raw.UserDeviceWithLocationRaw
import app.paralino.core.models.raw.UserDevicesWithLocationsAndPlacesRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
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
    private val deviceMapper: UserDeviceMapper,
    private val placeMapper: GroupPlaceMapperLocal,
    private val placeActivity: PlaceActivityDataSource,
    private val userRepo: UserRepository,
    private val remoteConfigRepo: RemoteConfigRepository,
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

    suspend fun recreateDevices(devices: List<UserDeviceWithLocationRaw>) {
        database.userDeviceDao.recreateUserDevices(
            devices = devices.map { deviceMapper.remote2Local(it) }
        )
    }

    suspend fun recreateDevicesWithPlaces(data: UserDevicesWithLocationsAndPlacesRaw) {
        recreateDevices(devices = data.devices)
        recreatePersonalPlaces(places = data.places)
    }

    suspend fun updateOrCreateDeviceWithLocation(device: UserDeviceWithLocationRaw) {
        database.userDeviceDao.createUserDevice(deviceMapper.remote2Local(device))
    }

    suspend fun updateOrCreateDevice(device: UserDeviceWithLocationRaw) {
        database.userDeviceDao.createUserDevice(deviceMapper.remote2Local(device))
    }

    suspend fun recreatePersonalPlaces(places: List<GroupPlaceWithSettingsRaw>) {
        val personalPlaces = places.filter { it.place.isInPersonalSpace == true }
        database.groupRelatedDao.recreateAllPlacesWithSettings(
            groupId = appPrefs.userId,
            placeEntity = personalPlaces.map { placeMapper.remote2Local(it.place) },
            placeSettings = personalPlaces.mapNotNull { it.settings?.let { placeMapper.remote2Local(it) } }
        )
        placeActivity.recreatePlaces(places = personalPlaces)
    }

    suspend fun updateOrCreatePlace(place: GroupPlaceWithSettingsRaw) {
        if (place.place.isInPersonalSpace == true) {
            database.groupRelatedDao.recreatePlaceWithSettings(
                placeEntity = placeMapper.remote2Local(place.place),
                placeSettings = place.settings?.let { placeMapper.remote2Local(it) }
            )
        }
    }

    fun getPersonalSpaceWithDevices(): Flow<UserPersonalSpaceModel?> {
        return combine(
            userRepo.localUser.map { it.data }.distinctUntilChanged(),
            getAllUserDevices(),
            userRepo.userPlans,
            database.groupRelatedDao.getPlacesCountForPersonalSpace()
        ) { user, devices, plan, placesCount ->
            if (user != null) {
                UserPersonalSpaceModel(
                    user = user,
                    plan = plan.highestValidPlanConfig,
                    devices = devices,
                    placesCount = placesCount ?: 0
                )
            } else {
                null
            }
        }
    }

    fun getAllUserDevices(): Flow<List<UserDeviceModel>> {
        return database.userDeviceDao.getAllUserDevices()
            .distinctFlowOfList()
            .map { entities ->
                entities.mapNotNull { entity ->
                    try {
                        deviceMapper.local2Core(entity)
                    } catch (e: Exception) {
                        // Log error and skip device if decryption fails
                        null
                    }
                }
            }
    }

    suspend fun getAllUserDevicesOnce(): List<UserDeviceModel> {
        return database.userDeviceDao.getAllUserDevicesOnce()
            .mapNotNull { entity ->
                try {
                    deviceMapper.local2Core(entity)
                } catch (e: Exception) {
                    // Log error and skip device if decryption fails
                    null
                }
            }
    }

    fun getUserDevice(deviceId: String): Flow<UserDeviceModel?> {
        return database.userDeviceDao.getUserDevice(deviceId)
            .map { entity ->
                entity?.let {
                    try {
                        deviceMapper.local2Core(it)
                    } catch (e: Exception) {
                        // Log error and return null if decryption fails
                        null
                    }
                }
            }
    }

    suspend fun getUserDeviceByIdOnce(id: String): UserDeviceModel? {
        return database.userDeviceDao.getUserDeviceOnce(id = id)?.let { entity ->
            try {
                deviceMapper.local2Core(entity)
            } catch (e: Exception) {
                // Log error and return null if decryption fails
                null
            }
        }
    }

    suspend fun getUserDeviceByDeviceIdOnce(deviceId: String): UserDeviceModel? {
        return database.userDeviceDao.getUserDeviceByDeviceIdOnce(deviceId)?.let { entity ->
            try {
                deviceMapper.local2Core(entity)
            } catch (e: Exception) {
                // Log error and return null if decryption fails
                null
            }
        }
    }

    suspend fun getUserDeviceBySessionIdOnce(sessionId: String): UserDeviceModel? {
        return database.userDeviceDao.getUserDeviceBySessionIdOnce(sessionId)?.let { entity ->
            try {
                deviceMapper.local2Core(entity)
            } catch (e: Exception) {
                // Log error and return null if decryption fails
                null
            }
        }
    }

    suspend fun updateUserDeviceData(device: UserDeviceRaw) {
        database.userDeviceDao.updateUserDeviceData(
            dataUpdate = UserDeviceEntityDeviceUpdate(
                id = device.id,
                deviceId = device.deviceId,
                pushTargetId = device.pushTargetId,
                sessionId = device.sessionId,
                ownerUserId = device.ownerUserId,
                locationPushToken = device.locationPushToken,
                deviceType = device.deviceType,
                encryptedSenderKey = device.encryptedSenderKey,
                userKeyVersion = device.userKeyVersion
            )
        )
    }

    suspend fun deleteUserDevice(deviceId: String) {
        database.userDeviceDao.deleteUserDevice(deviceId)
    }

    suspend fun deleteUserDeviceBySessionId(sessionId: String) {
        database.userDeviceDao.deleteUserDeviceBySessionId(sessionId)
    }

    suspend fun clearAllUserDevices() {
        database.userDeviceDao.clearAllUserDevices()
    }

    suspend fun clearLocationDataForDevice(deviceId: String) {
        database.userDeviceDao.clearLocationDataForDevice(deviceId)
    }

    suspend fun clearStatusForDevice(deviceId: String) {
        database.userDeviceDao.clearStatusForDevice(deviceId)
    }

    suspend fun deletePersonalPlace(placeId: String) {
        database.groupRelatedDao.deletePlaceData(placeId = placeId)
    }
} 