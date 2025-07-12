package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
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

class LocationHistoryViewModelDataController(
    val getLocationHistory: GetLocationHistoryUseCase,
    val refreshLocationHistory: RefreshLocationHistoryUseCase,
    val deleteLocationHistory: DeleteLocationHistoryEntryUseCase,
    val userRepo: UserRepository,
    val userGroupRepo: UserGroupLocalDataSource,
    val valueFormatter: IValueFormatter,
    val appPrefs: AppPrefs,
    val remoteConfig: RemoteConfigRepository,
    val geofenceManager: IGeofenceManager,
    val viewModel: LocationHistoryViewModel,
) {
    val _uiState = MutableStateFlow<LocationHistoryScreenState>(
        LocationHistoryScreenState.emptyState()
    )

    val _eventChannel = Channel<LocationHistoryScreenEvent?>()

    var historySubscriptionJob: Job? = null

    val dataFetchController = LocationHistoryDataFetchController(
        dataController = this
    )

    fun loadHistory(
        groupId: String,
        memberUserId: String
    ) {
        historySubscriptionJob?.cancel()
        historySubscriptionJob = dataFetchController.loadHistory(
            groupId = groupId,
            memberUserId = memberUserId
        )
            .launchIn(viewModel.coroutineScope)
    }

    fun generateDates(
        dateRangeDays: Int,
        startDate: LocalDate
    ): List<LocationHistoryDayState> {
        val dates = mutableListOf<LocationHistoryDayState>()
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Calculate which days are selected (dateRangeDays going back from startDate)
        val selectedDates = mutableSetOf<LocalDate>()
        repeat(dateRangeDays) {
            selectedDates.add(startDate.minus(DatePeriod(days = it)))
        }

        // Sort selected dates to calculate positions correctly
        val sortedSelectedDates = selectedDates.sorted()

        // Determine the earliest date to generate
        // Minimum: previous month start, but could be earlier if startDate is older
        val previousMonth = today.minus(DatePeriod(months = 1))
        val previousMonthStart = LocalDate(previousMonth.year, previousMonth.month, 1)
        val earliestDate = minOf(startDate, previousMonthStart)

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

    fun addPreviousMonth() {
        val currentState = _uiState.value
        val currentDays = currentState.days

        if (currentDays.isEmpty()) return

        // Find the oldest date in current days
        val oldestDate = currentDays.minByOrNull { it.date }?.date ?: return

        // Get the previous month
        val previousMonth = oldestDate.minus(DatePeriod(months = 1))
        val previousMonthStart = LocalDate(previousMonth.year, previousMonth.month, 1)
        val previousMonthEnd = LocalDate(oldestDate.year, oldestDate.month, 1)
            .minus(DatePeriod(days = 1))

        // Calculate which days are selected based on current selection
        val selectedDates = mutableSetOf<LocalDate>()
        // TODO check if selection == 1
        repeat(currentState.dateRangeDays) {
            selectedDates.add(currentState.selectedDate.minus(DatePeriod(days = it)))
        }
        val sortedSelectedDates = selectedDates.sorted()

        // Generate all days for the previous month
        val newDays = mutableListOf<LocationHistoryDayState>()
        var currentDate = previousMonthStart
        while (currentDate <= previousMonthEnd) {
            newDays.add(
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

        // Prepend new days to existing days and update state
        val updatedDays = newDays + currentDays
        _uiState.update {
            it.copy(days = updatedDays)
        }
    }

    fun handleDaySelection(
        dateRangeDays: Int,
        selectedDate: LocalDate
    ) {
        if (_uiState.value.isLoadingLocations) {
            return
        }

        val currentState = _uiState.value
        val currentRange = dateRangeDays

        // Calculate new start and end dates based on selected day and range
        val newStartDate = when (currentRange) {
            1 -> selectedDate
            2 -> selectedDate.minus(DatePeriod(days = 1))
            3 -> selectedDate.minus(DatePeriod(days = 2))
            5 -> selectedDate.minus(DatePeriod(days = 4))
            else -> selectedDate
        }

        // Update days with new selection
        val updatedDays = currentState.days.map { day ->
            val isInRange = day.date >= newStartDate && day.date <= selectedDate
            val position = when {
                !isInRange -> LocationHistorySelectedDayPosition.SOLO
                currentRange == 1 -> LocationHistorySelectedDayPosition.SOLO
                day.date == newStartDate -> LocationHistorySelectedDayPosition.START
                day.date == selectedDate -> LocationHistorySelectedDayPosition.END
                else -> LocationHistorySelectedDayPosition.CENTER
            }

            day.copy(
                isSelected = isInRange,
                position = position
            )
        }

        // Update state with new selection
        _uiState.update { state ->
            state.copy(
                dateRangeDays = dateRangeDays,
                selectedDate = selectedDate,
                days = updatedDays,
                isRefreshingData = true,
                isLoadingLocations = true,
                daysInLoading = state.daysInLoading.plus(updatedDays.filter { it.isSelected }.map { it.date }).distinct()
            )
        }

        // Reload history for the new date range
        historySubscriptionJob?.cancel()
        historySubscriptionJob = dataFetchController.reload(
            dateRangeDays = dateRangeDays,
            selectedDate = selectedDate
        )
            .launchIn(viewModel.coroutineScope)
    }

    private fun calculatePosition(
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

    fun selectDevice(device: UserActiveLocationDeviceState) {
        _uiState.update {
            val newState = it.copy(
                devices = it.devices.map {
                    when (it) {
                        is UserActiveLocationDeviceState.Known -> it.copy(isSelected = it.locationId == device.locationId)
                        is UserActiveLocationDeviceState.Unknown -> it.copy(isSelected = it.locationId == device.locationId)
                    }
                },
                selectedLocationId = null,
                deletionRangeStartIndex = null
            )

            val stats = LocationHistoryStats.calculateStats(
                state = newState,
                valueFormatter = valueFormatter,
                geofenceManager = geofenceManager
            )

            newState.copy(stats = stats)
        }
    }

    fun deleteSelectedEntry() {
        _uiState.value.deleteAction?.let {
            if (_uiState.value.isCurrentUser) {
                _uiState.value.selectedPoint?.let {
                    viewModel.coroutineScope.launch {
                        _uiState.update { it.copy(isDeletingEntry = true) }
                        try {
                            deleteLocationHistory.deleteEntries(
                                groupId = _uiState.value.groupId,
                                memberUserId = _uiState.value.memberUserId,
                                entries = listOf(it),
                                startDate = _uiState.value.startDate,
                                endDate = _uiState.value.endDate
                            )
                        } catch (e: Exception) {
                            viewModel.showSnackbar(e.errorString())
                        }
                        _uiState.update {
                            it.copy(isDeletingEntry = false, deletionRangeStartIndex = null)
                        }
                    }
                }
            }
        }
    }

    fun markForRangeDeletion() {
        _uiState.value.deleteAction?.let {
            if (_uiState.value.isCurrentUser) {
                _uiState.value.selectedLocationId?.let {
                    _uiState.update {
                        it.copy(deletionRangeStartIndex = _uiState.value.locationIndex)
                    }
                }
            }
        }
    }

    fun deleteSelectedRange() {
        _uiState.value.deleteAction?.let {
            val locations = _uiState.value.locationsToDelete
            if (_uiState.value.isCurrentUser && locations.isNotEmpty()) {
                _uiState.value.selectedPoint?.let {
                    viewModel.coroutineScope.launch {
                        _uiState.update { it.copy(isDeletingEntry = true) }
                        try {
                            deleteLocationHistory.deleteEntries(
                                groupId = _uiState.value.groupId,
                                memberUserId = _uiState.value.memberUserId,
                                entries = locations,
                                startDate = _uiState.value.startDate,
                                endDate = _uiState.value.endDate
                            )
                        } catch (e: Exception) {
                            viewModel.showSnackbar(e.errorString())
                        }
                        _uiState.update {
                            it.copy(isDeletingEntry = false, deletionRangeStartIndex = null)
                        }
                    }
                }
            }
        }
    }

    fun clear() {
        historySubscriptionJob?.cancel()
    }
    
//    fun processLocationHistory() {
//        val currentState = _uiState.value
//        val locations = currentState.locations
//
//        if (locations.isEmpty()) return
//
//        // Use the clustering service from the data fetch controller
//        val processedHistory = dataFetchController.clusteringService.processLocations(
//            points = locations,
//            mode = currentState.viewMode,
//            rangeStart = _uiState.value.startDate,
//            rangeEnd = _uiState.value.endDate
//        )
//
//        val newState = currentState.copy(processedHistory = processedHistory)
//
//        // Recalculate stats with the new processed history
//        val stats = LocationHistoryStats.calculateStats(
//            state = newState,
//            valueFormatter = valueFormatter,
//            geofenceManager = geofenceManager
//        )
//
//        _uiState.update {
//            newState.copy(stats = stats)
//        }
//    }
}