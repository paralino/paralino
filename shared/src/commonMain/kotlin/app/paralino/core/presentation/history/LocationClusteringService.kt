package app.paralino.core.presentation.history

import app.paralino.core.models.location.LocationHistoryPointLocationModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDate
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class LocationClusteringService(
    private val valueFormatter: IValueFormatter
) {

    fun processLocations(
        points: List<LocationHistoryPointState.Point>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocationHistoryPointState> {
        return clusterLocations(
            points = points,
            maxRadius = 100.0,
            minDwellTime = 5.minutes,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd
        )
    }

    private fun clusterLocations(
        points: List<LocationHistoryPointState.Point>,
        maxRadius: Double,
        minDwellTime: Duration,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocationHistoryPointState> {
        val clusters = mutableListOf<LocationHistoryPointState>()
        val sortedPoints = points.sortedBy { it.point.status.startTime }

        var currentCluster = mutableListOf<LocationHistoryPointState.Point>()

        for (point in sortedPoints) {
            if (point.point.loc == null) continue

            if (currentCluster.isEmpty()) {
                currentCluster.add(point)
            } else {
                val clusterCenter = calculateCenter(currentCluster)
                val distance = calculateDistance(point, clusterCenter)

                if (distance <= maxRadius) {
                    currentCluster.add(point)
                } else {
                    // Finalize current cluster and start new one
                    finalizeCluster(
                        points = currentCluster,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd
                    )?.let {
                        clusters.add(it)
                    }
                    currentCluster = mutableListOf(point)
                }
            }
        }

        // Last cluster
        finalizeCluster(
            points = currentCluster,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd
        )?.let {
            clusters.add(it)
        }

        return clusters
    }

    private fun finalizeCluster(
        points: List<LocationHistoryPointState.Point>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): LocationHistoryPointState? {
        if (points.size >= 2) {
            val startTime = points.first().point.status.startTime
            val endTime = points.last().point.status.endTime

            val center = calculateCenter(points)
            val radius = calculateRadius(points, center)

            return LocationHistoryPointState.Cluster(
                locationId = points.first().locationId,
                points = points,
                lat = center.lat,
                lng = center.lng,
                accuracy = radius.toFloat(),
                locationTime = valueFormatter.formatTimeRangeForLocationHistory(
                    startTimeUTC = startTime.isoToLocalDateTime(),
                    endTimeUTC = endTime.isoToLocalDateTime(),
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd
                ),
                battery = points.mapNotNull { it.point.status.bat }
                    .sorted()
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        listOf(it.first(), it.last())
                            .distinct()
                            .joinToString(separator = " - ") { "$it%" }
                }
            )

        } else if (points.size == 1) {
            return points.first()
        } else {
            return null
        }
    }

    private fun calculateCenter(points: List<LocationHistoryPointState.Point>): LatLng {
        val validPoints = points.mapNotNull { it.point.loc }
        return LatLng(
            lat = validPoints.map { it.lat }.average(),
            lng = validPoints.map { it.lng }.average()
        )
    }

    private fun calculateRadius(points: List<LocationHistoryPointState.Point>, center: LatLng): Double {
        return points.mapNotNull { it.point.loc }
            .maxOfOrNull { calculateDistance(it, center) }
            ?: 50.0
    }

    private fun calculateDistance(point: LocationHistoryPointState.Point, center: LatLng): Double {
        val loc = point.point.loc ?: return Double.MAX_VALUE
        return calculateDistance(loc, center)
    }

    private fun calculateDistance(loc: LocationHistoryPointLocationModel, center: LatLng): Double {
        // Simple distance calculation using Pythagorean theorem (flat earth approximation)
        val avgLat = (loc.lat + center.lat) / 2
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLng = 111320.0 * cos(avgLat * 0.01745329252)

        val deltaLat = (center.lat - loc.lat) * metersPerDegreeLat
        val deltaLng = (center.lng - loc.lng) * metersPerDegreeLng

        return sqrt(deltaLat * deltaLat + deltaLng * deltaLng)
    }
}

private data class LatLng(val lat: Double, val lng: Double)