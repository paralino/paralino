package app.paralino.core.repo.places

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.PlaceActivityEntity
import app.paralino.core.models.places.PlaceActivityState
import app.paralino.core.models.raw.GroupPlacesWithSettingsRaw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PlaceActivityDataSource(
    private val database: AppDatabaseHelper
) {

    private val _placeActivityCache = MutableStateFlow(mapOf<String, PlaceActivityState>())

    suspend fun getPlaceActivity(placeId: String): PlaceActivityState? {
        return getPlaceActivity()?.get(placeId)
    }

    suspend fun getPlaceActivity(): Map<String, PlaceActivityState>? {
        return _placeActivityCache.value.ifEmpty {
            database.executeWithResult { placeActivityQueries.getAll().executeAsList() }
                ?.associateBy { it.placeId }
                ?.mapValues {
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
                ?.also { _placeActivityCache.value = it }
                ?: emptyMap()
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
            database.placeActivityQueries.createPlaceActivity(
                placeActivityEntity = PlaceActivityEntity(
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
            database.execute {
                placeActivityQueries.deletePlaceActivity(placeId = placeId)
            }
        }
    }

    suspend fun recreatePlaces(places: List<GroupPlacesWithSettingsRaw>) {
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
        database.transaction {
            placeActivityQueries.getAll()
                .executeAsList()
                .forEach { activity ->
                    if (places.none { it.place.id == activity.placeId }) {
                        placeActivityQueries.deletePlaceActivity(placeId = activity.placeId)
                    }
                }
        }
    }

    suspend fun recreatePlacesForGroup(
        groupId: String,
        places: List<GroupPlacesWithSettingsRaw>
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
        database.transaction {
            placeActivityQueries.getAllForGroup(groupId = groupId)
                .executeAsList()
                .forEach { activity ->
                    if (places.none { it.place.id == activity.placeId }) {
                        placeActivityQueries.deletePlaceActivity(placeId = activity.placeId)
                    }
                }
        }
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
        database.execute { placeActivityQueries.deleteForGroup(groupId = groupId) }
    }

    suspend fun clearAll() {
        _placeActivityCache.update {
            it.forEach { it.value.tryNotifyJob?.cancel() }
            emptyMap()
        }
        database.execute {
            placeActivityQueries.clearAll()
        }
    }
}