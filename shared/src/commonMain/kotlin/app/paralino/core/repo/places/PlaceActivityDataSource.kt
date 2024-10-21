package app.paralino.core.repo.places

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.places.PlaceActivityEntity
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.models.raw.GroupPlaceWithSettingsRaw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PlaceActivityDataSource(
    private val database: MainDatabaseHelper
) {

    private val _placeActivityCache = MutableStateFlow(mapOf<String, PlaceActivityState>())

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
                        groupId = it.value.groupId,
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
                    groupId = state.groupId,
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
        _placeActivityCache.update {
            it.toMutableMap().apply {
                this.forEach { entry ->
                    if (places.none { it.place.id == entry.value.placeId }) {
                        remove(key = entry.key).also {
                            it?.tryNotifyJob?.cancel()
                        }
                    }
                }
            }
        }
        database.placeActivityDao.clearMissing(placeIds = places.map { it.place.id })
    }

    suspend fun recreatePlacesForGroup(
        groupId: String,
        places: List<GroupPlaceWithSettingsRaw>
    ) {
        _placeActivityCache.update {
            it.toMutableMap().apply {
                this.filter { it.value.groupId == groupId }.forEach { entry ->
                    if (places.none { it.place.id == entry.value.placeId }) {
                        remove(key = entry.key).also {
                            it?.tryNotifyJob?.cancel()
                        }
                    }
                }
            }
        }
        database.placeActivityDao.clearMissing(groupId = groupId, placeIds = places.map { it.place.id })
    }

    suspend fun clearForGroup(groupId: String) {
        _placeActivityCache.update {
            it.toMutableMap().apply {
                this.filter { it.value.groupId == groupId }.forEach { entry ->
                    remove(key = entry.key).also {
                        it?.tryNotifyJob?.cancel()
                    }
                }
            }
        }
        database.placeActivityDao.deleteForGroup(groupId = groupId)
    }

    suspend fun clearAll() {
        _placeActivityCache.update {
            it.forEach { it.value.tryNotifyJob?.cancel() }
            emptyMap()
        }
        database.placeActivityDao.clearAll()
    }
}