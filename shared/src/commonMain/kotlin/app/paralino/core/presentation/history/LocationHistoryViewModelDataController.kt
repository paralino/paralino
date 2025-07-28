package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
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
    val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
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

    fun addPreviousMonth() {
        val currentState = _uiState.value
        val currentDays = currentState.daySelectorState.days

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
        repeat(currentState.daySelectorState.dateRangeDays ?: 0) {
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
                    position = dataFetchController.calculatePosition(currentDate, sortedSelectedDates)
                )
            )
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        // Prepend new days to existing days and update state
        val updatedDays = newDays + currentDays
        _uiState.update {
            it.copy(daySelectorState = it.daySelectorState.copy(days = updatedDays))
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

        if (!currentState.canSelectDate(selectedDate)) {
            viewModel.showSnackbar(text = MR.strings.history_map_date_unselectable_error.localizedString())
            return
        }

        // Calculate new start and end dates based on selected day and range
        val newStartDate = when (dateRangeDays) {
            1 -> selectedDate
            2 -> selectedDate.minus(DatePeriod(days = 1))
            3 -> selectedDate.minus(DatePeriod(days = 2))
            5 -> selectedDate.minus(DatePeriod(days = 4))
            else -> selectedDate
        }.let {
            if (currentState.daySelectorState.earliestDate != null) {
                it.coerceAtLeast(currentState.daySelectorState.earliestDate)
            } else {
                it
            }
        }

        // Update days with new selection
        val updatedDays = currentState.daySelectorState.days.map { day ->
            val isInRange = day.date >= newStartDate && day.date <= selectedDate
            val currentRange = (selectedDate - newStartDate).days + 1
            val position = when {
                !isInRange -> LocationHistorySelectedDayPosition.SOLO
                currentRange <= 1 -> LocationHistorySelectedDayPosition.SOLO
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
                daySelectorState = state.daySelectorState.copy(
                    days = updatedDays,
                    daysInLoading = state.daySelectorState.daysInLoading
                        .plus(updatedDays.filter { it.isSelected }.map { it.date })
                        .distinct(),
                    dateRangeDays = dateRangeDays,
                ),
                selectedDate = selectedDate,
                isLoadingLocations = true,
                isFilterOpened = false
            )
        }

        // Reload history for the new date range
        historySubscriptionJob?.cancel()
        historySubscriptionJob = dataFetchController.reload(
            startDate = _uiState.value.startDate,
            endDate = _uiState.value.endDate
        )
            .launchIn(viewModel.coroutineScope)
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
                                groupId = _uiState.value.groupOrSpaceId,
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
                                groupId = _uiState.value.groupOrSpaceId,
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
}