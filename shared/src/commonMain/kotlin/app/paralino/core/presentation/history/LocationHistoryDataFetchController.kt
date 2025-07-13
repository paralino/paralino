package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.Logr
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.listOfDatesUntil
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.extensions.stripNanoseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocationHistoryDataFetchController(
    val dataController: LocationHistoryViewModelDataController,
) {

    private val getLocationHistory: GetLocationHistoryUseCase = dataController.getLocationHistory
    private val refreshLocationHistory: RefreshLocationHistoryUseCase =
        dataController.refreshLocationHistory
    private val userRepo: UserRepository = dataController.userRepo
    private val userGroupRepo: UserGroupLocalDataSource = dataController.userGroupRepo
    private val valueFormatter: IValueFormatter = dataController.valueFormatter
    private val appPrefs: AppPrefs = dataController.appPrefs
    private val remoteConfig: RemoteConfigRepository = dataController.remoteConfig
    private val _uiState = dataController._uiState
    
    private val _clusteringService = LocationClusteringService(valueFormatter)
    val clusteringService: LocationClusteringService get() = _clusteringService

    fun loadHistory(
        groupId: String,
        memberUserId: String
    ): Flow<Unit> {
        _uiState.update {
            val selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            it.copy(
                groupId = groupId,
                memberUserId = memberUserId,
                isPersonalHistory = groupId == UserGroup.groupPersonalDevicesId,
                isCurrentUser = memberUserId == appPrefs.userId,
                historyDaysLimit = null,
                isGroupOwner = false,
                dateRangeDays = 1,
                selectedDate = selectedDate,
                locations = emptyList(),
                devices = emptyList(),
                isDeletingEntry = false,
                deletionRangeStartIndex = null,
                isRefreshingData = true,
                isLoadingLocations = true,
                stats = null,
                selectedLocationId = null,
                days = dataController.generateDates(
                    dateRangeDays = 1,
                    startDate = selectedDate
                ),
                daysInLoading = listOf(selectedDate)
            )
        }

        return reload(
            dateRangeDays = _uiState.value.dateRangeDays,
            selectedDate = _uiState.value.selectedDate
        )
    }

    fun reload(
        dateRangeDays: Int,
        selectedDate: LocalDate
    ): Flow<Unit> {
        val groupId = _uiState.value.groupId
        val memberUserId = _uiState.value.memberUserId
        val startDate = _uiState.value.startDate
        val endDate = _uiState.value.endDate

        val job = if (groupId == UserGroup.groupPersonalDevicesId) {
            fetchPersonalLocationHistory(
                startDate = startDate,
                endDate = endDate
            )
        } else {
            fetchGroupLocationHistory(
                groupId = groupId,
                memberUserId = memberUserId,
                startDate = startDate,
                endDate = endDate
            )
        }

        dataController.viewModel.coroutineScope.launch {
            try {
                refreshLocationHistory.refreshLocationHistory(
                    groupId = groupId,
                    memberUserId = memberUserId,
                    locationId = null,
                    startDate = startDate,
                    endDate = endDate,
                    requestCacheDuration = 2.minutes
                )
            } catch (e: Exception) {
                // TODO handle error
                logg.e(message = "Error refreshing location history: ${e.message}", t = e)
                dataController.viewModel.showSnackbar(text = e.errorString())
            }

            _uiState.update {
                it.copy(
                    isRefreshingData = false,
                    daysInLoading = it.daysInLoading - startDate.listOfDatesUntil(endDate)
                )
            }
        }

        return job
    }

    private fun fetchPersonalLocationHistory(startDate: LocalDate, endDate: LocalDate): Flow<Unit> {
        return combine(
            userRepo.localUser,
            userRepo.userPlans,
            getLocationHistory.getLocationHistory(
                groupId = UserGroup.groupPersonalDevicesId,
                memberUserId = appPrefs.userId,
                startDate = startDate,
                endDate = endDate
            )
        ) { user, plans, history ->
            // TODO add actual devices and locations
            // TODO handle user missing
            val locations = history.history.flatMap {
                mapLocationPoints(entry = it)
            }
            
            val processedLocations = _clusteringService.processLocations(
                points = locations,
                rangeStart = _uiState.value.startDate,
                rangeEnd = _uiState.value.endDate
            )
            
            _uiState.update {
                it.copy(
                    groupName = null,
                    groupImage = null,
                    historyDaysLimit = plans.highestValidPlanConfig.personalHistoryDaysLimit,
                    isGroupOwner = true,
                    userName = user.data?.name ?: "",
                    userImage = user.data?.data?.userImage,
                    isLoadingLocations = false,
                    locations = processedLocations,
                    devices = emptyList(),
                    stats = null,
                    dataViewType = LocationHistoryDataViewType.Stats
                )
            }
        }
            .catch { }
    }

    private fun fetchGroupLocationHistory(
        groupId: String,
        memberUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Unit> {
        return getLocationHistory.getLocationHistory(
            groupId = groupId,
            memberUserId = memberUserId,
            startDate = startDate,
            endDate = endDate
        ).map { history ->
            val group = userGroupRepo.getGroupWithMembersOnce(groupId = groupId)
            val currentMember = group?.memberByUserId(userId = memberUserId)
            if (group != null && currentMember != null) {

                val knownDevices = currentMember.locations.map {
                    UserActiveLocationDeviceState.Known(
                        locationId = it.id,
                        deviceId = it.deviceId,
                        deviceName = it.name,
                        isSelected = it.id == _uiState.value.devices.firstOrNull { it.isSelected }?.locationId,
                        isCurrent = it.id == group.currentDeviceLocation?.id
                    )
                }

                val unknownDevices = history.history.filter { entry ->
                    knownDevices.none { it.locationId == entry.locationId } || entry.locationId == null
                }
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        UserActiveLocationDeviceState.Unknown(
                            ids = it.map { it.locationId }.distinct(),
                            deviceName = "Unknown",
                            isSelected = _uiState.value.devices.firstOrNull { it.isSelected } is UserActiveLocationDeviceState.Unknown,
                        )
                    }

                val devices = buildList {
                    addAll(knownDevices)

                    if (unknownDevices != null) {
                        add(unknownDevices)
                    }
                }.let { list ->
                    if (list.count { it.isSelected } != 1) {
                        val firstSelected = list.firstOrNull()
                        list.map {
                            when (it) {
                                is UserActiveLocationDeviceState.Known -> it.copy(isSelected = it.locationId == firstSelected?.locationId)
                                is UserActiveLocationDeviceState.Unknown -> it.copy(isSelected = it.locationId == firstSelected?.locationId)
                            }
                        }
                    } else {
                        list
                    }
                }

                val selectedKnownLocation = currentMember.locations
                    .firstOrNull { location ->
                        devices
                            .filter { it.isSelected && it is UserActiveLocationDeviceState.Known }
                            .any { it.locationId == location.id }
                    }

                val cachedHistory = selectedKnownLocation?.locationHistory?.data?.let {
                    it.history
                        .filter {
                            val date = it.status.tA.isoToLocalDateTime()?.date
                            if (date == null) {
                                false
                            } else {
                                date in _uiState.value.startDate.._uiState.value.endDate
                            }
                        }
                        .groupBy { it.dayKey }
                        .mapNotNull { entry ->
                            entry.key?.let {
                                LocationHistoryModelForDevice(
                                    locationId = selectedKnownLocation.id,
                                    entryId = selectedKnownLocation.id,
                                    dateKey = it,
                                    historyKeyVersion = "",
                                    isCachedLocation = true,
                                    history = CryptoResource.Ready(LocationHistoryModel(history = entry.value))
                                )
                            }
                        }
                }

                val locations = history.history
                    .run {
                        if (cachedHistory != null) plus(cachedHistory)
                        else this
                    }
                    .flatMap { entry ->
                        mapLocationPoints(entry = entry)
                    }
                    .sortedByDescending { it.point.status.startTime }
                    .distinctBy { it.point.properID }

                val processedLocations = _clusteringService.processLocations(
                    points = locations,
                    rangeStart = _uiState.value.startDate,
                    rangeEnd = _uiState.value.endDate
                )

                val newState = _uiState.value.copy(
                    groupName = group.name,
                    groupImage = group.image,
                    historyDaysLimit = group.group.historyDaysLimit(remoteConfig.getConfigOnce()),
                    isGroupOwner = group.isCurrentUserOwner,
                    userName = currentMember.member.name,
                    userImage = currentMember.member.image
                        ?: ImageModel.emoji(Emojis.robot),
                    isLoadingLocations = false,
                    locations = processedLocations,
                    devices = devices,
                    stats = null,
                    // TODO adjust focused item
                    dataViewType = _uiState.value.dataViewType,
                )

                val stats = LocationHistoryStats.calculateStats(
                    state = newState,
                    valueFormatter = dataController.valueFormatter,
                    geofenceManager = dataController.geofenceManager,
                )

                _uiState.update {
                    newState.copy(stats = stats)
                }
            } else {
                // TODO handle data missing
                _uiState.update {
                    it.copy(
                        isLoadingLocations = false
                    )
                }
            }
        }
            .catch {
                Logr.d("Error loading history", it)
            }
            .flowOn(Dispatchers.IO)
    }

    private fun mapLocationPoints(entry: LocationHistoryModelForDevice): List<LocationHistoryPointState.Point> {
        return entry.history.data?.history
            ?.map { point ->
                val locATime = point.loc?.startTime?.isoToLocalDateTime()?.stripNanoseconds()
                val locBTime = point.loc?.endTime?.isoToLocalDateTime()?.stripNanoseconds()
                val statusATime = point.status.startTime.isoToLocalDateTime()?.stripNanoseconds()
                val statusBTime = point.status.endTime.isoToLocalDateTime()?.stripNanoseconds()

                val locationTime = valueFormatter.formatTimeRangeForLocationHistory(
                    startTimeUTC = locATime,
                    endTimeUTC = locBTime,
                    rangeStart = _uiState.value.startDate,
                    rangeEnd = _uiState.value.endDate,
                )

                val statusTime = if (locBTime != statusBTime) {
                    valueFormatter.formatTimeRangeForLocationHistory(
                        // using same time to just indicate last point for status update
                        startTimeUTC = statusATime.takeIf { it != locATime },
                        endTimeUTC = statusBTime,
                        rangeStart = _uiState.value.startDate,
                        rangeEnd = _uiState.value.endDate,
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
                            meters = it.toDouble()
                        )
                    },
                    deviceStatus = MemberDeviceStatusType.fromShortId(id = point.status.sts),
                    networkType = NetworkType.fromKey(key = point.status.net),
                    signalStrength = NetworkSignalStrength.fromLevel(level = point.status.sig)
                )
            }
            ?: emptyList()
    }
}

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
    
    private fun calculateDistance(loc: app.paralino.core.models.location.LocationHistoryPointLocationModel, center: LatLng): Double {
        // Simple distance calculation using Pythagorean theorem (flat earth approximation)
        val avgLat = (loc.lat + center.lat) / 2
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLng = 111320.0 * kotlin.math.cos(avgLat * 0.01745329252)

        val deltaLat = (center.lat - loc.lat) * metersPerDegreeLat
        val deltaLng = (center.lng - loc.lng) * metersPerDegreeLng

        return kotlin.math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng)
    }
}

private data class LatLng(val lat: Double, val lng: Double)