package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.Logr
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

class LocationHistoryDataFetchController(
    val dataController: LocationHistoryViewModelDataController,
) {

    private val getLocationHistory: GetLocationHistoryUseCase = dataController.getLocationHistory
    private val refreshLocationHistory: RefreshLocationHistoryUseCase =
        dataController.refreshLocationHistory
    private val userRepo: UserRepository = dataController.userRepo
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource = dataController.personalSpaceRepo
    private val userGroupRepo: UserGroupLocalDataSource = dataController.userGroupRepo
    private val valueFormatter: IValueFormatter = dataController.valueFormatter
    private val appPrefs: AppPrefs = dataController.appPrefs
    private val remoteConfig: RemoteConfigRepository = dataController.remoteConfig
    private val _uiState = dataController._uiState
    
    private val clusteringService = LocationClusteringService(valueFormatter)

    fun loadHistory(
        groupId: String,
        memberUserId: String
    ): Flow<Unit> {
        return flow {
            val selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val isPersonalHistory = groupId == UserGroup.groupPersonalDevicesId
            val isCurrentUser = memberUserId.isBlank() || memberUserId == appPrefs.userId

            val newState = _uiState.value.copy(
                groupOrSpaceId = groupId.asGroupOrSpaceId(),
                memberUserId = memberUserId.takeIf { it.isNotBlank() } ?: appPrefs.userId,
                isPersonalHistory = isPersonalHistory,
                isCurrentUser = isCurrentUser,
                isGroupOwner = false,
                selectedDate = selectedDate,
                locations = emptyList(),
                devices = emptyList(),
                isDeletingEntry = false,
                deletionRangeStartIndex = null,
                isLoadingLocations = true,
                isLoadingData = true,
                stats = null,
                daySelectorState = createDateSelector(
                    groupId = groupId,
                    selectedDate = selectedDate,
                    dateRangeDays = 1,
                    daysInLoading = listOf(selectedDate)
                ),
                selectedLocationId = null,
            )

            _uiState.value = newState

            emitAll(
                reload(
                    startDate = newState.startDate,
                    endDate = newState.endDate
                )
            )
        }
    }

    fun reload(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Unit> {
        val groupId = _uiState.value.groupOrSpaceId
        val memberUserId = _uiState.value.memberUserId

        val job = if (groupId.isPersonalSpace) {
            fetchPersonalLocationHistory(
                startDate = startDate,
                endDate = endDate
            )
        } else {
            fetchGroupLocationHistory(
                groupId = groupId.id,
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
                logg.e(message = "Error refreshing location history: ${e.message}", t = e)
                dataController.viewModel.showSnackbar(text = e.errorString())
            }

            _uiState.update {
                it.copy(
                    daySelectorState = it.daySelectorState.copy(
                        daysInLoading = it.daySelectorState.daysInLoading - startDate.listOfDatesUntil(endDate)
                    )
                )
            }
        }

        return job
    }

    private fun fetchPersonalLocationHistory(startDate: LocalDate, endDate: LocalDate): Flow<Unit> {
        return getLocationHistory.getLocationHistory(
            groupId = UserGroup.groupPersonalDevicesId,
            memberUserId = appPrefs.userId,
            startDate = startDate,
            endDate = endDate
        ).map { history ->
            val space = personalSpaceRepo.personalSpaceState.value.data
            if (space != null) {
                val user = space.user

                val knownDevices = space.devices.map {
                    UserActiveLocationDeviceState.Known(
                        locationId = it.id,
                        deviceId = it.deviceId,
                        deviceName = it.location?.nameOrNull
                            // TODO read device name
                            ?: it.deviceType?.toString()
                            ?: MR.strings.history_map_device_name_unknown.localizedString(),
                        isSelected = it.id == _uiState.value.devices.firstOrNull { it.isSelected }?.locationId,
                        isCurrent = it.deviceId == space.currentDevice?.deviceId
                    )
                }

                val unknownDevices = history.history.filter { entry ->
                    knownDevices.none { it.locationId == entry.locationId } || entry.locationId == null
                }
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        UserActiveLocationDeviceState.Unknown(
                            ids = it.map { it.locationId }.distinct(),
                            deviceName = MR.strings.history_map_device_name_unknown.localizedString(),
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

                val selectedKnownLocation = space.devices
                    .firstOrNull { device ->
                        devices
                            .filter { it.isSelected && it is UserActiveLocationDeviceState.Known }
                            .any { it.locationId == device.location?.id }
                    }

                val cachedHistory = selectedKnownLocation?.location?.locationHistory?.data?.let {
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

                val processedLocations = clusteringService.processLocations(
                    points = locations,
                    rangeStart = _uiState.value.startDate,
                    rangeEnd = _uiState.value.endDate
                )

                val newState = _uiState.value.copy(
                    groupName = MR.strings.personalSpace_spaceTitle.localizedString(),
                    groupImage = user.data?.userImage,
                    isGroupOwner = true,
                    userName = user.name,
                    userImage = user.data?.userImage
                        ?: ImageModel.emoji(Emojis.robot),
                    isLoadingLocations = false,
                    isLoadingData = false,
                    locations = processedLocations,
                    devices = devices,
                    stats = null,
                    planConfig = space.plan,
                    dataViewType = _uiState.value.dataViewType
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
                            deviceName = MR.strings.history_map_device_name_unknown.localizedString(),
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

                val processedLocations = clusteringService.processLocations(
                    points = locations,
                    rangeStart = _uiState.value.startDate,
                    rangeEnd = _uiState.value.endDate
                )

                val newState = _uiState.value.copy(
                    groupName = group.name,
                    groupImage = group.image,
                    isGroupOwner = group.isCurrentUserOwner,
                    userName = currentMember.member.name,
                    userImage = currentMember.member.image
                        ?: ImageModel.emoji(Emojis.robot),
                    isLoadingLocations = false,
                    isLoadingData = false,
                    locations = processedLocations,
                    devices = devices,
                    stats = null,
                    planConfig = group.group.planConfig(remoteConfig.getConfigOnce()),
                    dataViewType = _uiState.value.dataViewType
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

    private suspend fun createDateSelector(
        groupId: String,
        selectedDate: LocalDate,
        dateRangeDays: Int,
        daysInLoading: List<LocalDate>,
    ): LocationHistoryDateSelectorState {
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isPersonalHistory = groupId == UserGroup.groupPersonalDevicesId
        val isGroupOwner = if (isPersonalHistory) {
            true
        } else {
            userGroupRepo.allGroupsState.value.data?.firstOrNull { it.id == groupId }
                ?.group?.isCurrentUserOwner ?: false
        }

        val planConfig = if (isPersonalHistory) {
            userRepo.userPlans.value.highestValidPlanConfig
        } else {
            userGroupRepo.allGroupsState.value.data?.firstOrNull { it.id == groupId }
                ?.group?.planConfig(remoteConfig.getConfigOnce())
                ?: PlanConfig.default
        }

        val earliestDate = if (isPersonalHistory) {
            if (planConfig.personalHistoryDaysLimit >= 0) {
                today.minus(DatePeriod(days = planConfig.personalHistoryDaysLimit))
            } else {
                null
            }
        } else {
            if (planConfig.historyDaysLimit >= 0) {
                today.minus(DatePeriod(days = planConfig.historyDaysLimit))
            } else {
                null
            }
        }

        val highestPlan = remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order } ?: PlanConfig.default
        val upgradeAvailable = highestPlan.planTypeEnum.order > planConfig.planTypeEnum.order
        val couldLoadMore = earliestDate == null

        val startAction = if (couldLoadMore) {
            LocationHistoryDateSelectionAction.LOAD_MODE
        } else if (upgradeAvailable) {
            if (isGroupOwner) {
                LocationHistoryDateSelectionAction.UPGRADE
            } else {
                LocationHistoryDateSelectionAction.ASK_FOR_UPGRADE
            }
        } else {
            LocationHistoryDateSelectionAction.END
        }

        return LocationHistoryDateSelectorState(
            days = generateDates(
                dateRangeDays = dateRangeDays,
                startDate = selectedDate,
                earliestDate = earliestDate
            ),
            daysInLoading = daysInLoading,
            dateRangeDays = dateRangeDays,
            startAction = startAction,
            earliestDate = earliestDate
        )
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

    fun generateDates(
        dateRangeDays: Int,
        startDate: LocalDate,
        earliestDate: LocalDate?,
    ): List<LocationHistoryDayState> {
        val dates = mutableListOf<LocationHistoryDayState>()
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Calculate which days are selected (dateRangeDays going back from startDate)
        val selectedDates = mutableSetOf<LocalDate>()
        repeat(dateRangeDays) {
            startDate.minus(DatePeriod(days = it)).takeIf {
                if (earliestDate != null) {
                    it >= earliestDate
                } else {
                    true
                }
            }?.let {
                selectedDates.add(it)
            }
        }

        // Sort selected dates to calculate positions correctly
        val sortedSelectedDates = selectedDates.sorted()

        // Determine the earliest date to generate
        // Minimum: previous month start, but could be earlier if startDate is older
        val earliestDate = if (earliestDate == null) {
            val previousMonth = today.minus(DatePeriod(months = 1))
            val previousMonthStart = LocalDate(previousMonth.year, previousMonth.month, 1)
            minOf(startDate, previousMonthStart)
        } else {
            earliestDate
        }

        // Generate all days from earliest date to today
        var currentDate = earliestDate
        while (currentDate <= today) {
            dates.add(
                LocationHistoryDayState(
                    isSelected = selectedDates.contains(currentDate),
                    isLoading = false,
                    date = currentDate,
                    formattedMonth = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "MMM"
                    ),
                    formattedDay = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "EEE"
                    ),
                    isWeekend = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY,
                    position = calculatePosition(currentDate, sortedSelectedDates)
                )
            )
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        return dates
    }

    fun calculatePosition(
        date: LocalDate,
        sortedSelectedDates: List<LocalDate>
    ): LocationHistorySelectedDayPosition {
        if (!sortedSelectedDates.contains(date)) {
            return LocationHistorySelectedDayPosition.SOLO
        }

        // If only one day is selected, it's SOLO
        if (sortedSelectedDates.size == 1) {
            return LocationHistorySelectedDayPosition.SOLO
        }

        val index = sortedSelectedDates.indexOf(date)

        return when {
            index == 0 -> LocationHistorySelectedDayPosition.START
            index == sortedSelectedDates.size - 1 -> LocationHistorySelectedDayPosition.END
            else -> LocationHistorySelectedDayPosition.CENTER
        }
    }
}