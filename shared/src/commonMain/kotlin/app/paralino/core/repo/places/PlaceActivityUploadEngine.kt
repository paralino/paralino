package app.paralino.core.repo.places

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.executeWithResult
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class PlaceActivityUploadEngine(
    private val remote: PlacesRemoteSource,
    private val local: PlacesLocalDataSource,
    private val database: AppDatabaseHelper,
    private val scopeProvider: ICoroutineScopeProvider
) : BaseRepository() {

    private val executionKey = "place_activity_pending_sync"

    suspend fun notifyPlaceEvent(
        placeId: String,
        groupId: String,
        isEnter: Boolean,
        timeMillis: Long
    ) {
        try {
            sendPlaceEventImpl(
                placeId = placeId,
                groupId = groupId,
                isEnter = isEnter,
                timeMillis = timeMillis
            )

            // sync pending events, but on different thread so we do not block current flow
            scopeProvider.userSessionIOScope.launch {
                syncPendingEvents()
            }
        } catch (e: Exception) {
            logg.tag(executionKey)
                .w("Error sending place activity event for place (${placeId}), create pending event - ${e.message}")
            database.pendingPlaceEventsQueries.createPendingEvent(
                placeId = placeId,
                groupId = groupId,
                isEnter = isEnter,
                notifyTimeMillis = timeMillis
            )
        }
    }

    suspend fun syncPendingEvents(): Result<Unit> {
        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = executionKey,
                    timeMillis = 10.minutes.inWholeMilliseconds,
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
        val pendingEvents = database.executeWithResult {
            pendingPlaceEventsQueries.getAll().executeAsList()
        } ?: emptyList()
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
                        .v("Place ${event.placeId} exists, sync pending place event (id: ${event.id})")
                    try {
                        sendPlaceEventImpl(
                            placeId = event.placeId,
                            groupId = event.groupId,
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
                        .v("Place ${event.placeId} does not exists anymore, cancel pending place event (id: ${event.id})")
                    return@mapNotNull event.id
                }
            }

        logg.tag(executionKey).v("Sent ${sentEvents.size} pending place events")

        database.transaction {
            sentEvents.forEach {
                try {
                    pendingPlaceEventsQueries.deleteEvent(id = it)
                } catch (_: Exception) {
                    // ignore if any
                }
            }
            pendingPlaceEventsQueries.deleteObsoleteEvents(
                maxPendingTimeMillis = maxPendingTimeMillis
            )
        }
    }

    private suspend fun sendPlaceEventImpl(
        placeId: String,
        groupId: String,
        isEnter: Boolean,
        timeMillis: Long
    ) {
        if (isEnter) {
            remote.notifyPlaceEnter(
                placeId = placeId,
                groupId = groupId,
                timeMillis = timeMillis
            )
        } else {
            remote.notifyPlaceExit(
                placeId = placeId,
                groupId = groupId,
                timeMillis = timeMillis
            )
        }
    }
}