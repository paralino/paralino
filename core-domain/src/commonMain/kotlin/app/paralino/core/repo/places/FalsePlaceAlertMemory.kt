package app.paralino.core.repo.places

import app.paralino.core.logg
import app.paralino.core.models.location.LatLng
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * In some areas, device keeps getting "thrown" (bad Wi-Fi/cell positioning) to seemingly random locations.
 * But they keep repeating themselves, i.e. it is same location spot all over again.
 * They have good reported accuracy, which then schedules a place alert and boosts location, over and over.
 * It is impossible to just filter based on accuracy, so here we try to detect a pattern of those places.
 *
 * - Learn: a place change is flipped back shortly after by a location far from the one that caused it,
 *   so the first location is remembered as inaccurate spot candidate. Seen twice, it is a known inaccurate spot.
 * - Filter: a location near a known inaccurate spot is not allowed to change place state (no schedule, no boost).
 *   They are not set in stone, a longer stay near the spot (several agreeing locations over some time) still goes through
 *   and makes the memory forget the spot, so a wrongly learned spot heals itself.
 */
class FalsePlaceAlertMemory(
    private val dataSource: FalsePlaceAlertLocationDataSource,
    private val geofenceManager: IGeofenceManager,
) {

    enum class FixVerdict {
        PROCESS, IGNORE
    }

    private data class Trigger(val isEnter: Boolean, val point: LatLng, val time: Instant)
    private data class Streak(val count: Int, val first: Instant, val last: Instant)

    private val mutex = Mutex()
    private var isLoaded = false
    private val spots =
        mutableMapOf<String, List<FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation>>()
    private val triggers = mutableMapOf<String, Trigger>()
    private val streaks = mutableMapOf<String, Streak>()

    suspend fun processLocation(
        placeId: String,
        previousIsEnter: Boolean?,
        movementIsEnter: Boolean?,
        location: LatLng,
    ): FixVerdict = mutex.withLock {
        if (previousIsEnter == null || movementIsEnter == null) {
            return@withLock FixVerdict.PROCESS
        }

        val now = nowUTC()
        if (!ensureLoaded(now)) {
            return@withLock FixVerdict.PROCESS
        }

        if (movementIsEnter == previousIsEnter) {
            // location agrees with current state
            streaks.remove(placeId)
            return@withLock FixVerdict.PROCESS
        }

        // location wants to change the state
        val isReversal = learn(
            placeId = placeId,
            isEnter = movementIsEnter,
            location = location,
            now = now
        )

        // for now we check with at least 2 locations, review later if we maybe need more
        val knownSpot = findSpot(
            placeId = placeId,
            isEnter = movementIsEnter,
            point = location,
            now = now,
            minHits = 2
        )

        if (knownSpot == null) {
            streaks.remove(placeId)
            // a reversal (usually the real location after a phantom) is not a new trigger,
            // otherwise the next phantom would reverse it and the real location would be learned as a phantom
            if (!isReversal) {
                triggers[placeId] = Trigger(isEnter = movementIsEnter, point = location, time = now)
            }
            return@withLock FixVerdict.PROCESS
        }

        val previous = streaks[placeId]?.takeIf { now - it.last <= 15.minutes }
        val streak = if (previous != null) {
            previous.copy(count = previous.count + 1, last = now)
        } else {
            Streak(count = 1, first = now, last = now)
        }

        if (streak.count >= 3 && now - streak.first >= 2.minutes) {
            // several locations over some time. this is where the device really is, probably.
            // so the spot was learned wrongly (or is not inaccurate anymore)
            logg.tag("place_activity")
                .i("Known inaccurate spot confirmed by ${streak.count} locations, forget spot - $placeId")
            streaks.remove(placeId)
            val remaining = activeSpots(placeId, now).filter { it != knownSpot }
            spots[placeId] = remaining
            persist(placeId = placeId, placeSpots = remaining)
            return@withLock FixVerdict.PROCESS
        }

        streaks[placeId] = streak
        keepSpotAlive(placeId = placeId, spot = knownSpot, now = now)
        logg.tag("place_activity")
            .i("Ignore location near known inaccurate spot (${streak.count}) - $placeId")
        FixVerdict.IGNORE
    }

    private suspend fun learn(
        placeId: String,
        isEnter: Boolean,
        location: LatLng,
        now: Instant
    ): Boolean {
        val trigger = triggers[placeId] ?: return false
        if (trigger.isEnter == isEnter || now - trigger.time > 5.minutes) {
            return false
        }

        triggers.remove(placeId)

        if (distance(trigger.point, location) < 150) {
            // jitter, not a jump
            return true
        }

        val placeSpots = activeSpots(placeId, now)
        val existing =
            findSpot(placeId = placeId, isEnter = trigger.isEnter, point = trigger.point, now = now)
        val updated = if (existing != null) {
            placeSpots.map {
                if (it == existing) it.copy(
                    lastSeenMillis = now.toEpochMilliseconds(),
                    hits = it.hits + 1
                ) else it
            }
        } else {
            (placeSpots + FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation(
                lat = trigger.point.lat,
                lng = trigger.point.lng,
                isEnter = trigger.isEnter,
                lastSeenMillis = now.toEpochMilliseconds(),
                hits = 1
            ))
                .sortedByDescending { it.lastSeenMillis }
                // don't take too many potential fake spots into consideration
                .take(10)
        }

        logg.tag("place_activity")
            .i("Remember inaccurate spot (hits: ${(existing?.hits ?: 0) + 1}) - $placeId")
        spots[placeId] = updated
        persist(placeId = placeId, placeSpots = updated)
        return true
    }

    private suspend fun keepSpotAlive(
        placeId: String,
        spot: FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation,
        now: Instant
    ) {
        val lastSeen = Instant.fromEpochMilliseconds(spot.lastSeenMillis)
        if (now - lastSeen < 30.minutes) {
            return
        }

        val updated = activeSpots(placeId, now).map {
            if (it == spot) {
                it.copy(lastSeenMillis = now.toEpochMilliseconds())
            } else {
                it
            }
        }
        spots[placeId] = updated
        persist(placeId = placeId, placeSpots = updated)
    }

    private suspend fun ensureLoaded(now: Instant): Boolean {
        if (isLoaded) {
            return true
        }

        val stored = try {
            dataSource.loadAll()
        } catch (e: Exception) {
            logg.tag("place_activity").w("Error loading inaccurate spots: ${e.message}")
            return false
        }

        stored.forEach { (placeId, placeSpots) ->
            val active = placeSpots.filter { isActive(it, now) }
            if (active.isEmpty()) {
                persist(placeId = placeId, placeSpots = emptyList())
            } else {
                spots[placeId] = active
            }
        }
        isLoaded = true
        return true
    }

    private suspend fun persist(
        placeId: String,
        placeSpots: List<FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation>
    ) {
        try {
            dataSource.save(placeId = placeId, falseLocations = placeSpots)
        } catch (e: Exception) {
            logg.tag("place_activity").w("Error saving inaccurate spots - $placeId: ${e.message}")
        }
    }

    private fun activeSpots(
        placeId: String,
        now: Instant
    ): List<FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation> {
        return spots[placeId].orEmpty().filter { isActive(it, now) }
    }

    private fun isActive(
        spot: FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation,
        now: Instant
    ): Boolean {
        return now - Instant.fromEpochMilliseconds(spot.lastSeenMillis) <= 12.hours
    }

    private fun findSpot(
        placeId: String,
        isEnter: Boolean,
        point: LatLng,
        now: Instant,
        minHits: Int = 1
    ): FalsePlaceAlertLocationDataSource.FalsePlaceAlertLocation? {
        return activeSpots(placeId, now)
            .filter { it.isEnter == isEnter && it.hits >= minHits }
            .map { it to distance(LatLng(lat = it.lat, lng = it.lng), point) }
            .filter { it.second <= 150 } // 150 meters
            .minByOrNull { it.second }
            ?.first
    }

    private fun distance(a: LatLng, b: LatLng): Float {
        return geofenceManager.distanceBetweenPoints(
            lat1 = a.lat,
            lng1 = a.lng,
            lat2 = b.lat,
            lng2 = b.lng
        )
    }

    suspend fun clear() {
        mutex.withLock {
            spots.clear()
            triggers.clear()
            streaks.clear()
            isLoaded = false
        }
    }
}
