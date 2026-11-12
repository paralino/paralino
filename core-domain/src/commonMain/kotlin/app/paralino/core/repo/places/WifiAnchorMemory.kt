package app.paralino.core.repo.places

import app.paralino.core.logg
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.provider.ConnectedWifi
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.places.WifiAnchorDataSource.Sample
import app.paralino.core.repo.places.WifiAnchorDataSource.WifiAnchor
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WifiAnchorMemory(
    private val dataSource: WifiAnchorDataSource,
    private val geofenceManager: IGeofenceManager,
) {

    private data class ExitStreak(val count: Int, val first: Instant)

    private val mutex = Mutex()
    private var anchors: List<WifiAnchor>? = null
    private val exitStreaks = mutableMapOf<String, ExitStreak>()

    suspend fun processLocation(
        wifi: ConnectedWifi?,
        location: LocationDataModel
    ) {
        mutex.withLock {
            // metered is usually a hotspot, it can move with the device
            if (wifi == null || wifi.isMetered) {
                return@withLock
            }

            if (location.accuracy > 50) {
                return@withLock
            }

            val locationTime = location.timeUTCInstant?.toEpochMilliseconds() ?: return@withLock
            val all = getAnchors() ?: return@withLock

            val existingWifi = all.firstOrNull { it.bssid == wifi.bssid }
            val lastSample = existingWifi?.samples?.lastOrNull()

            if (lastSample != null && locationTime - lastSample.timeMillis < 10.minutes.inWholeMilliseconds) {
                // only track locations over time, not bursts
                return@withLock
            }

            val newSample = Sample(
                lat = location.lat,
                lng = location.lng,
                timeMillis = locationTime
            )

            val anchor = WifiAnchor(
                id = existingWifi?.id ?: dataSource.newId(),
                bssid = wifi.bssid,
                samples = (existingWifi?.samples.orEmpty() + newSample).takeLast(20)
            )
            val expiry = (nowUTC() - 60.days).toEpochMilliseconds()
            val updated =
                (all.filter { it.bssid != wifi.bssid && it.samples.last().timeMillis > expiry } + anchor)
                    .sortedByDescending { it.samples.last().timeMillis }
                    .take(50)

            anchors = updated

            try {
                all.filter { old -> updated.none { it.id == old.id } }
                    .forEach { dataSource.delete(id = it.id) }
                dataSource.save(anchor)
            } catch (e: Exception) {
                logg.tag("place_activity").w("Error saving wifi anchor: ${e.message}")
            }
            logg.tag("place_activity")
                .d("Wifi anchor sample (${anchor.samples.size}, valid: ${wifiCenter(anchor) != null}) - ${wifi.bssid.takeLast(5)}")
        }
    }

    suspend fun isOnPlaceWifi(
        wifi: ConnectedWifi?,
        placeCenter: LatLng,
        radiusMeters: Float
    ): Boolean = mutex.withLock {
        isAnchorInPlace(wifi = wifi, placeCenter = placeCenter, radiusMeters = radiusMeters)
    }

    suspend fun shouldIgnoreExit(
        placeId: String,
        wifi: ConnectedWifi?,
        placeCenter: LatLng,
        radiusMeters: Float
    ): Boolean = mutex.withLock {
        if (!isAnchorInPlace(wifi = wifi, placeCenter = placeCenter, radiusMeters = radiusMeters)) {
            exitStreaks.remove(placeId)
            return@withLock false
        }

        val now = nowUTC()
        val previous = exitStreaks[placeId]?.takeIf { now - it.first < 1.hours }
        val streak = previous?.copy(count = previous.count + 1)
            ?: ExitStreak(count = 1, first = now)

        // lots of location updates outside this wifi zone, accept it
        if (streak.count >= 8 && now - streak.first >= 5.minutes) {
            exitStreaks.remove(placeId)
            logg.tag("place_activity").i("Place exit confirmed despite wifi - $placeId")
            return@withLock false
        }

        exitStreaks[placeId] = streak
        logg.tag("place_activity")
            .i("Ignore place exit - on place wifi (${streak.count}) - $placeId")
        true
    }

    suspend fun resetExit(placeId: String) {
        mutex.withLock {
            exitStreaks.remove(placeId)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            anchors = null
            exitStreaks.clear()
        }
    }

    private suspend fun isAnchorInPlace(
        wifi: ConnectedWifi?,
        placeCenter: LatLng,
        radiusMeters: Float
    ): Boolean {
        if (wifi == null) {
            return false
        }
        val anchor = getAnchors()?.firstOrNull { it.bssid == wifi.bssid } ?: return false
        val center = wifiCenter(anchor) ?: return false
        return distance(center, placeCenter) <= radiusMeters
    }

    // null if wifi location is not known well enough yet, or wifi moves around
    private fun wifiCenter(anchor: WifiAnchor): LatLng? {
        val samples = anchor.samples
        if (samples.size < 5 || samples.last().timeMillis - samples.first().timeMillis < 1.hours.inWholeMilliseconds) {
            return null
        }
        val center = LatLng(
            lat = samples.map { it.lat }.sorted()[samples.size / 2],
            lng = samples.map { it.lng }.sorted()[samples.size / 2]
        )
        val nearCenter = samples.count {
            distance(center, LatLng(lat = it.lat, lng = it.lng)) <= 100
        }
        return center.takeIf { nearCenter >= samples.size * 0.8 }
    }

    private suspend fun getAnchors(): List<WifiAnchor>? {
        anchors?.let { return it }
        return try {
            dataSource.loadAll().also { anchors = it }
        } catch (e: Exception) {
            logg.tag("place_activity").w("Error loading wifi anchors: ${e.message}")
            null
        }
    }

    private fun distance(a: LatLng, b: LatLng): Float {
        return geofenceManager.distanceBetweenPoints(
            lat1 = a.lat,
            lng1 = a.lng,
            lat2 = b.lat,
            lng2 = b.lng
        )
    }
}
