package app.paralino.core.presentation.history

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.stripSeconds
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class ILocationHistoryStateMapper(
    private val valueFormatter: IValueFormatter,
    private val geofenceManager: IGeofenceManager
) {
    fun mapLocationPoints(
        entry: LocationHistoryModelForDevice,
        startDate: LocalDate,
        endDate: LocalDate,
        timeZone: TimeZone,
        nowDate: LocalDateTime,
        yesterday: LocalDate
    ): List<LocationHistoryPointState.Point> {
        return entry.history.data?.history
            ?.map { point ->
                val locATime = point.loc?.startTime?.isoToLocalDateTime()?.stripSeconds()
                val locBTime = point.loc?.endTime?.isoToLocalDateTime()?.stripSeconds()
                val statusBTime = point.status.endTime.isoToLocalDateTime()?.stripSeconds()

                val locationTime = valueFormatter.formatTimeRangeForLocationHistory(
                    timeZone = timeZone,
                    nowDate = nowDate,
                    yesterday = yesterday,
                    startTimeUTC = locATime,
                    endTimeUTC = locBTime,
                    rangeStart = startDate,
                    rangeEnd = endDate,
                )

                val statusTime = if (locBTime != statusBTime) {
                    val statusATime = point.status.startTime.isoToLocalDateTime()?.stripSeconds()
                    valueFormatter.formatTimeRangeForLocationHistory(
                        timeZone = timeZone,
                        nowDate = nowDate,
                        yesterday = yesterday,
                        // using same time to just indicate last point for status update
                        startTimeUTC = statusATime.takeIf { it != locATime },
                        endTimeUTC = statusBTime,
                        rangeStart = startDate,
                        rangeEnd = endDate,
                    )
                } else {
                    null
                }

                LocationHistoryPointState.Point(
                    locationId = entry.locationId,
                    entryId = entry.entryId,
                    dateKey = entry.dateKey,
                    isCachedLocation = entry.isCachedLocation,
                    point = point,
                    locationTime = locationTime,
                    statusTime = statusTime.takeIf { it != locationTime },
                    formattedSpeed = point.loc?.spd?.let {
                        valueFormatter.getSpeedSmall(
                            metersPerSecond = it
                        )
                    },
                    formattedElevation = point.loc?.alt?.let {
                        valueFormatter.getDistanceSmall(
                            meters = it.toDouble(),
                            distanceType = DistanceType.ELEVATION
                        )
                    },
                    deviceStatus = MemberDeviceStatusType.fromShortId(id = point.status.sts),
                    networkType = NetworkType.fromKey(key = point.status.net),
                    signalStrength = NetworkSignalStrength.fromLevel(level = point.status.sig)
                )
            }
            ?: emptyList()
    }

    fun mapFinalState(
        currentState: LocationHistoryScreenState,
        groupName: String?,
        groupImage: ImageModel?,
        isGroupOwner: Boolean,
        userName: String?,
        userImage: ImageModel?,
        locations: List<LocationHistoryPointState>,
        devices: List<UserActiveLocationDeviceState>,
        planConfig: PlanConfig
    ): LocationHistoryScreenState {
        val newState = currentState.copy(
            preselectedDeviceId = null,
            groupName = groupName,
            groupImage = groupImage,
            isGroupOwner = isGroupOwner,
            userName = userName,
            userImage = userImage ?: ImageModel.emoji(Emojis.robot),
            isLoadingLocations = false,
            isLoadingData = false,
            locations = locations,
            devices = devices,
            stats = null,
            planConfig = planConfig
        )

        val stats = LocationHistoryStats.calculateStats(
            state = newState,
            valueFormatter = valueFormatter,
            geofenceManager = geofenceManager,
        )

        // currently disabled since it causes too much delay/processing on the UI
        
//        val newPointClustering = when (newState.clusteringPointsMode) {
//            LocationHistoryClusteringPointsViewMode.DEFAULT_CLUSTERING,
//            LocationHistoryClusteringPointsViewMode.DEFAULT_DETAILED -> {
//                if ((stats?.locationEntriesCount ?: 0) > 50) {
//                    LocationHistoryClusteringPointsViewMode.DEFAULT_CLUSTERING
//                } else {
//                    LocationHistoryClusteringPointsViewMode.DEFAULT_DETAILED
//                }
//            }
//            else -> newState.clusteringPointsMode
//        }
//
//        val newPinClustering = when (newState.clusteringPinsMode) {
//            LocationHistoryClusteringPinsViewMode.DEFAULT_CLUSTERING,
//            LocationHistoryClusteringPinsViewMode.DEFAULT_DETAILED -> {
//                val count = if (newPointClustering.isClusteringOn) {
//                    newState.selectedLocations.size
//                } else {
//                    (stats?.locationEntriesCount ?: 0)
//                }
//
//                if (count > 50) {
//                    LocationHistoryClusteringPinsViewMode.DEFAULT_CLUSTERING
//                } else {
//                    LocationHistoryClusteringPinsViewMode.DEFAULT_DETAILED
//                }
//            }
//            else -> newState.clusteringPinsMode
//        }

        return newState.copy(
            stats = stats,
//            clusteringPinsMode = newPinClustering,
//            clusteringPointsMode = newPointClustering
        )
    }
}