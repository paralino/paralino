package app.paralino.core.repo.places

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.places.PendingPlaceEventsEntity
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class PlaceActivityUploadEngine(
    private val remote: PlacesRemoteSource,
    private val local: PlacesLocalDataSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val remoteConfig: RemoteConfigRepository,
    private val database: MainDatabaseHelper,
    private val scopeProvider: ICoroutineScopeProvider
) : BaseRepository() {

    private val executionKey = "place_activity_pending_sync"

    suspend fun notifyPlaceEvent(
        place: GroupPlace,
        isEnter: Boolean,
        timeMillis: Long
    ) {
        try {
            sendPlaceEventImpl(
                place = place,
                isEnter = isEnter,
                timeMillis = timeMillis
            )
            logg.tag(executionKey)
                .i(
                    "Sent place activity ${if (isEnter) "enter" else "exit"} event for place (${place.id}) at ${
                        Instant.fromEpochMilliseconds(timeMillis)
                    }"
                )

            // sync pending events, but on different thread so we do not block current flow
            syncPendingEventsInBackground()
        } catch (e: Exception) {
            logg.tag(executionKey)
                .w("Error sending place activity ${if (isEnter) "enter" else "exit"} event for place (${place.id}), create pending event - ${e.message}")
            database.groupRelatedDao.createPendingEvent(
                PendingPlaceEventsEntity(
                    id = 0,
                    placeId = place.id,
                    groupId = place.groupId,
                    isEnter = isEnter,
                    notifyTimeMillis = timeMillis
                )
            )
        }
    }

    fun syncPendingEventsInBackground() {
        scopeProvider.userSessionIOScope.launch {
            syncPendingEventsNow()
        }
    }

    suspend fun syncPendingEventsNow(): Result<Unit> {
        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = executionKey,
                    timeMillis = 10.seconds.inWholeMilliseconds,
                    failOnError = false
                ),
                remote = { syncPendingEventsImpl() }
            )
        } catch (e: Exception) {
            logg.tag(executionKey).w("Error syncing pending place events - ${e.message}")
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    private suspend fun syncPendingEventsImpl() {
        val pendingEvents = database.groupRelatedDao.getAllPendingEventsOnce()
        val maxPendingTimeMillis = nowUTC().minus(12.hours).toEpochMilliseconds()

        val sentEvents = pendingEvents
            .filter { it.notifyTimeMillis > maxPendingTimeMillis }
            .also {
                logg.tag(executionKey)
                    .v("Found ${pendingEvents.size} pending place events, ${it.size} still valid")
            }
            .mapNotNull { event ->
                val place = local.getPlaceWithoutSettingsOnce(placeId = event.placeId)
                if (place != null) {
                    logg.tag(executionKey)
                        .i("Place ${event.placeId} exists, sync pending place event (id: ${event.id})")
                    try {
                        sendPlaceEventImpl(
                            place = place,
                            isEnter = event.isEnter,
                            timeMillis = event.notifyTimeMillis
                        )
                        return@mapNotNull event.id
                    } catch (e: Exception) {
                        logg.tag(executionKey)
                            .w("Error syncing pending place event (id: ${event.id}) for place (${event.placeId})- ${e.message}")
                        return@mapNotNull null
                    }
                } else {
                    logg.tag(executionKey)
                        .i("Place ${event.placeId} does not exists anymore, cancel pending place event (id: ${event.id})")
                    return@mapNotNull event.id
                }
            }

        logg.tag(executionKey).v("Sent ${sentEvents.size} pending place events")

        database.groupRelatedDao.updatePendingEventsData(
            sentEvents = sentEvents,
            maxPendingTimeMillis = maxPendingTimeMillis
        )
    }

    private suspend fun sendPlaceEventImpl(
        place: GroupPlace,
        isEnter: Boolean,
        timeMillis: Long
    ) {
        try {
            val group = groupLocal.allGroupsState.value.data?.firstOrNull {
                it.group.id == place.groupId
            }
            if (group?.arePlacesOverLimits(config = remoteConfig.getConfigOnce()) == true) {
                logg.tag(executionKey).v("Group places are over limit, skip alert")
                return
            }
        } catch (e: Exception) {
        }

        if (isEnter) {
            remote.notifyPlaceEnter(
                place = place,
                timeMillis = timeMillis
            )
        } else {
            remote.notifyPlaceExit(
                place = place,
                timeMillis = timeMillis
            )
        }
    }
}