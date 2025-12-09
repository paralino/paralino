package app.paralino.core.presentation.history

import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.DistanceType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.stripSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

class ILocationHistoryStateMapper(
    private val valueFormatter: IValueFormatter,
    private val geofenceManager: IGeofenceManager,
    private val clusteringService: LocationClusteringService,
    private val _uiState: MutableStateFlow<LocationHistoryScreenState>
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
        startDate: LocalDate,
        endDate: LocalDate,
        groupName: String?,
        groupImage: ImageModel?,
        isGroupOwner: Boolean,
        userName: String?,
        userImage: ImageModel?,
        history: LocationHistoryModelForUser,
        cachedHistory: List<LocationHistoryModelForDevice>,
        devices: List<UserActiveLocationDeviceState>,
        planConfig: PlanConfig
    ) {

        logg.v("history_fetch", "Devices mapped, map points")

        val localTimezone = TimeZone.currentSystemDefault()
        val now = nowUTC()
        val nowDate = now.toLocalDateTime(localTimezone)
        val yesterday = now.minus(1.days).toLocalDateTime(localTimezone).date
        val locations = history.history
            .plus(cachedHistory)
            .flatMap { entry ->
                mapLocationPoints(
                    entry = entry,
                    startDate = startDate,
                    endDate = endDate,
                    timeZone = localTimezone,
                    nowDate = nowDate,
                    yesterday = yesterday,
                )
            }
            .sortedByDescending { it.point.status.startTime }
            .distinctBy { it.point.properID }

        logg.v("history_fetch", "Cluster locations")
        val processedLocations = clusteringService.processLocations(
            oldPoints = _uiState.value.locations,
            newPoints = locations,
            rangeStart = startDate,
            rangeEnd = endDate
        )

        _uiState.update { currentState ->
            val newState = currentState.copy(
                preselectedDeviceId = null,
                groupName = groupName,
                groupImage = groupImage,
                isGroupOwner = isGroupOwner,
                userName = userName,
                userImage = userImage ?: ImageModel.emoji(Emojis.robot),
                isLoadingLocations = false,
                isLoadingData = false,
                locations = processedLocations,
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

            val newPinClustering = when (newState.clusteringPinsMode) {
                LocationHistoryClusteringPinsViewMode.DEFAULT_CLUSTERING,
                LocationHistoryClusteringPinsViewMode.DEFAULT_DETAILED -> {
                    val count = if (newState.clusteringPointsMode.isClusteringOn) {
                        newState.selectedLocations.size
                    } else {
                        (stats?.locationEntriesCount ?: 0)
                    }

                    if (count >= 80) {
                        LocationHistoryClusteringPinsViewMode.DEFAULT_CLUSTERING
                    } else {
                        LocationHistoryClusteringPinsViewMode.DEFAULT_DETAILED
                    }
                }

                else -> newState.clusteringPinsMode
            }

            newState.copy(
                stats = stats,
                clusteringPinsMode = newPinClustering,
//            clusteringPointsMode = newPointClustering
            )
        }
    }
}