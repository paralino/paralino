package app.paralino.core.presentation.history

import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.extensions.isoToLocalDateTime

data class LocationHistoryStats(
    val formattedTimeRange: String?,
    val totalDistanceMeters: Float,
    val batteryRange: Pair<Int, Int>,
    val averageBattery: Int,
    val maxSpeedMps: Float?,
    val elevationRangeMeters: Pair<Float, Float>,
    val elevationGainMeters: Float,
    val locationEntriesCount: Int,
    val networks: List<String>,
    val formattedDistance: String,
    val formattedMaxSpeed: String,
    val formattedElevationRange: String,
    val formattedElevationGain: String
) {
    companion object {
        fun calculateStats(
            state: LocationHistoryScreenState,
            valueFormatter: IValueFormatter,
            geofenceManager: IGeofenceManager
        ): LocationHistoryStats? {
            val locations = state.selectedLocations
                .flatMap {
                    when (it) {
                        is LocationHistoryPointState.Cluster -> it.points
                        is LocationHistoryPointState.Point -> listOf(it)
                    }
                }

            if (locations.isEmpty()) {
                return null
            }

            // Get location points with valid coordinates required for some data
            val locationPoints = locations.mapNotNull { it.point.loc }.sortedBy { it.startTime }

            val formattedTimeRange = valueFormatter.formatTimeRangeForLocationHistory(
                startTimeUTC = locations.first().point.status.startTime.isoToLocalDateTime(),
                endTimeUTC = locations.last().point.status.endTime.isoToLocalDateTime(),
                rangeStart = state.startDate,
                rangeEnd = state.endDate
            )

            var totalDistanceMeters = 0f
            for (i in 1 until locationPoints.size) {
                val prev = locationPoints[i - 1]
                val curr = locationPoints[i]
                totalDistanceMeters += geofenceManager.distanceBetweenPoints(
                    lat1 = prev.lat,
                    lng1 = prev.lng,
                    lat2 = curr.lat,
                    lng2 = curr.lng
                )
            }

            val batteryLevels = locations.mapNotNull { it.point.status.bat }
            val batteryRange = if (batteryLevels.isNotEmpty()) {
                Pair(batteryLevels.minOrNull() ?: 0, batteryLevels.maxOrNull() ?: 0)
            } else {
                Pair(0, 0)
            }
            val averageBattery = if (batteryLevels.isNotEmpty()) {
                batteryLevels.average().toInt()
            } else {
                0
            }

            val maxSpeedMps = locationPoints.mapNotNull { it.spd }.maxOrNull()

            val elevations = locationPoints.mapNotNull { it.alt }
            val elevationRangeMeters = if (elevations.isNotEmpty()) {
                Pair(elevations.minOrNull() ?: 0f, elevations.maxOrNull() ?: 0f)
            } else {
                Pair(0f, 0f)
            }

            val elevationGainMeters = if (elevations.size >= 2) {
                elevations.last() - elevations.first()
            } else {
                0f
            }

            val networks = locations
                    .mapNotNull { it.point.status.net?.let { NetworkType.Companion.fromKey(it)?.networkName?.takeIf { it.isNotBlank() } } }
                    .distinct()

            // Format values
            val formattedDistance = valueFormatter.getDistanceSmall(totalDistanceMeters.toDouble(), distanceType = DistanceType.DISTANCE)
            val formattedMaxSpeed = maxSpeedMps?.let { valueFormatter.getSpeedSmall(it) } ?: "-"
            val formattedElevationRange =
                "${valueFormatter.getDistanceSmall(
                    meters = elevationRangeMeters.first.toDouble(),
                    distanceType = DistanceType.ELEVATION
                )} - ${
                    valueFormatter.getDistanceSmall(
                        meters = elevationRangeMeters.second.toDouble(),
                        distanceType = DistanceType.ELEVATION
                    )
                }"

            val formattedElevationGain =
                valueFormatter.getDistanceSmall(
                    meters = elevationGainMeters.toDouble(),
                    distanceType = DistanceType.ELEVATION
                )

            return LocationHistoryStats(
                formattedTimeRange = formattedTimeRange,
                totalDistanceMeters = totalDistanceMeters,
                batteryRange = batteryRange,
                averageBattery = averageBattery,
                maxSpeedMps = maxSpeedMps,
                elevationRangeMeters = elevationRangeMeters,
                elevationGainMeters = elevationGainMeters,
                locationEntriesCount = locations.size,
                networks = networks,
                formattedDistance = formattedDistance,
                formattedMaxSpeed = formattedMaxSpeed,
                formattedElevationRange = formattedElevationRange,
                formattedElevationGain = formattedElevationGain
            )
        }
    }
}