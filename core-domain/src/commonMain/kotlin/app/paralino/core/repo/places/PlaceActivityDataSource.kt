package app.paralino.core.repo.places

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.places.PlaceActivityEntity
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaceActivityDataSource(
    private val database: MainDatabaseHelper,
    private val falseAlertMemory: FalsePlaceAlertMemory,
) {

    private val _placeActivityCache = MutableStateFlow(mapOf<String, PlaceActivityState>())
    private val mutex = Mutex()

    suspend fun getPlaceActivity(placeId: String): PlaceActivityState? {
        return getPlaceActivity()?.get(placeId)
    }

    suspend fun getPlaceActivity(): Map<String, PlaceActivityState>? {
        return _placeActivityCache.value.ifEmpty {
            database.placeActivityDao.getAll()
                .associateBy { it.placeId }
                .mapValues {
                    PlaceActivityState(
                        placeId = it.value.placeId,
                        groupId = it.value.groupId.asGroupOrSpaceId(),
                        isEnter = it.value.isEnter,
                        stateChangeTrigger = PlaceActivityTriggerType.fromKey(key = it.value.stateChangeTrigger),
                        stateChangeTimeMillis = it.value.stateChangeTimeMillis,
                        isScheduledStateEnter = it.value.isScheduledStateEnter,
                        isLastNotifyStateEnter = it.value.isLastNotifyStateEnter,
                        notifyTimeMillis = it.value.notifyTimeMillis,
                        scheduleTrigger = it.value.scheduleTrigger?.let {
                            PlaceActivityTriggerType.fromKey(key = it)
                        },
                        notifyTrigger = it.value.notifyTrigger?.let {
                            PlaceActivityTriggerType.fromKey(key = it)
                        },
                        tryNotifyJob = _placeActivityCache.value.get(it.key)?.tryNotifyJob
                    )
                }
                .also { _placeActivityCache.value = it }
        }
    }

    suspend fun updatePlaceActivity(placeId: String, state: PlaceActivityState?) {
        _placeActivityCache.update {
            it.toMutableMap().apply {
                if (state != null) {
                    set(placeId, state)
                } else {
                    remove(placeId).also {
                        it?.tryNotifyJob?.cancel()
                    }
                }
            }
        }

        if (state != null) {
            database.placeActivityDao.createPlaceActivity(
                PlaceActivityEntity(
                    placeId = placeId,
                    groupId = state.groupId.id,
                    isInPersonalSpace = state.groupId.isPersonalSpace,
                    isEnter = state.isEnter,
                    stateChangeTrigger = state.stateChangeTrigger.key,
                    stateChangeTimeMillis = state.stateChangeTimeMillis,
                    isScheduledStateEnter = state.isScheduledStateEnter,
                    isLastNotifyStateEnter = state.isLastNotifyStateEnter,
                    notifyTimeMillis = state.notifyTimeMillis,
                    scheduleTrigger = state.scheduleTrigger?.key,
                    notifyTrigger = state.notifyTrigger?.key,
                )
            )
        } else {
            database.placeActivityDao.deletePlaceActivity(placeId = placeId)
        }
    }

    suspend fun recreatePlaces(places: List<GroupPlaceWithSettingsRaw>) {
        mutex.withLock {
            _placeActivityCache.update { state ->
                val toRemove = state.filter { entry ->
                    places.none { it.place.id == entry.value.placeId }
                }
                val newState = state.filter { entry ->
                    toRemove.none { it.key == entry.key }
                }
                toRemove.forEach { it.value.tryNotifyJob?.cancel() }
                newState
            }
            database.placeActivityDao.clearMissingForGroups(placeIds = places.map { it.place.id })
        }
        removeMissingFalseLocations()
    }

    suspend fun recreatePlacesForGroup(
        groupId: GroupOrSpaceId,
        places: List<GroupPlaceWithSettingsRaw>
    ) {
        mutex.withLock {
            _placeActivityCache.update { state ->
                val toRemove = state.filter { entry ->
                    entry.value.groupId == groupId && places.none { it.place.id == entry.value.placeId }
                }
                val newState = state.filter { entry ->
                    toRemove.none { it.key == entry.key }
                }
                toRemove.forEach { it.value.tryNotifyJob?.cancel() }
                newState
            }
            database.placeActivityDao.clearMissing(groupId = groupId.id, placeIds = places.map { it.place.id })
        }
        removeMissingFalseLocations()
    }

    suspend fun clearForGroup(groupId: GroupOrSpaceId) {
        _placeActivityCache.update { state ->
            val toRemove = state.filter { entry ->
                entry.value.groupId == groupId
            }
            val newState = state.filter { entry ->
                toRemove.none { it.key == entry.key }
            }
            toRemove.forEach { it.value.tryNotifyJob?.cancel() }
            newState
        }
        database.placeActivityDao.deleteForGroup(groupId = groupId.id)
        removeMissingFalseLocations()
    }

    private suspend fun removeMissingFalseLocations() {
        try {
            falseAlertMemory.removeMissing(placeIds = database.groupRelatedDao.getAllPlacesOnce().map { it.id }.toSet())
        } catch (e: Exception) {
            // ignore - false locations expire on their own
        }
    }

    suspend fun clearCache() {
        _placeActivityCache.update {
            it.forEach { it.value.tryNotifyJob?.cancel() }
            emptyMap()
        }
    }
}