package app.paralino.core.presentation.history

import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationHistoryPointLocationModel
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class LocationClusteringService(
    private val valueFormatter: IValueFormatter
) {

    fun processLocations(
        oldPoints: List<LocationHistoryPointState>,
        newPoints: List<LocationHistoryPointState.Point>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocationHistoryPointState> {
        return newPoints
            .groupBy { it.locationId }
            .values
            .flatMap { devicePoints ->
                clusterLocations(
                    oldPoints = oldPoints,
                    newPoints = devicePoints,
                    maxRadius = 100.0,
                    minDwellTime = 5.minutes,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd
                )
            }
            .sortedBy { item ->
                when (item) {
                    is LocationHistoryPointState.Cluster ->
                        item.points.first().point.status.startTime
                    is LocationHistoryPointState.Point ->
                        item.point.status.startTime
                }
            }
    }

    private fun clusterLocations(
        oldPoints: List<LocationHistoryPointState>,
        newPoints: List<LocationHistoryPointState.Point>,
        maxRadius: Double,
        minDwellTime: Duration,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocationHistoryPointState> {
        val now = nowUTC()
        val timeZone = TimeZone.currentSystemDefault()
        val nowDate = now.toLocalDateTime(timeZone)
        val yesterday = now.minus(1.days).toLocalDateTime(timeZone).date

        val clusters = mutableListOf<LocationHistoryPointState>()
        val sortedPoints = newPoints.sortedBy { it.point.loc?.startTime ?: it.point.status.startTime }

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
                        oldPoints = oldPoints,
                        newPoints = currentCluster,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        timeZone = timeZone,
                        nowDate = nowDate,
                        yesterday = yesterday,
                    ).let {
                        clusters.addAll(it)
                    }
                    currentCluster = mutableListOf(point)
                }
            }
        }

        // Last cluster
        finalizeCluster(
            oldPoints = oldPoints,
            newPoints = currentCluster,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            timeZone = timeZone,
            nowDate = nowDate,
            yesterday = yesterday,
        ).let {
            clusters.addAll(it)
        }

        return clusters
    }

    private fun finalizeCluster(
        oldPoints: List<LocationHistoryPointState>,
        newPoints: List<LocationHistoryPointState.Point>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        timeZone: TimeZone,
        nowDate: LocalDateTime,
        yesterday: LocalDate
    ): List<LocationHistoryPointState> {
        if (newPoints.size >= 3) {
            val startTime = newPoints.first().point.status.startTime
            val endTime = newPoints.last().point.status.endTime

            val center = calculateCenter(newPoints)
            val radius = calculateRadius(newPoints, center)
            val locationId = newPoints.first().locationId
            val pointId = newPoints.first().point.properID

            val cluster = LocationHistoryPointState.Cluster(
                locationId = locationId,
                pointId = pointId,
                points = newPoints,
                lat = center.lat,
                lng = center.lng,
                accuracy = radius.toFloat(),
                locationTime = valueFormatter.formatTimeRangeForLocationHistory(
                    startTimeUTC = startTime.isoToLocalDateTime(),
                    endTimeUTC = endTime.isoToLocalDateTime(),
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    timeZone = timeZone,
                    nowDate = nowDate,
                    yesterday = yesterday
                ),
                battery = newPoints.mapNotNull { it.point.status.bat }
                    .sorted()
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        listOf(it.first(), it.last())
                            .distinct()
                            .joinToString(separator = " - ") { "$it%" }
                },
                isExpanded = oldPoints
                    .filterIsInstance<LocationHistoryPointState.Cluster>()
                    .firstOrNull { it.pointId == pointId }
                    ?.isExpanded
                    ?: false
            )
            return listOf(cluster)
        } else {
            return newPoints
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