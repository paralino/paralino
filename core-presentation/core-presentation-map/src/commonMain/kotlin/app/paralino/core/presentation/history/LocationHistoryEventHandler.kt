package app.paralino.core.presentation.history

import app.paralino.core.resources.moko.*
import app.paralino.core.domain.location.history.DeleteLocationHistoryEntryUseCase
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.absoluteValue

class LocationHistoryEventHandler(
    private val deleteLocationHistory: DeleteLocationHistoryEntryUseCase,
    private val valueFormatter: IValueFormatter,
    private val geofenceManager: IGeofenceManager,
    private val dataFetchController: LocationHistoryDataFetchController,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val coroutineScope: CoroutineScope,
    private val historyDateMapper: LocationHistoryDateMapper,
    private val _uiState: MutableStateFlow<LocationHistoryScreenState>,
) {

    var historySubscriptionJob: Job? = null

    fun loadHistory(
        groupId: String,
        memberUserId: String,
        preselectedDeviceId: String?
    ) {
        historySubscriptionJob?.cancel()
        historySubscriptionJob = dataFetchController.loadHistory(
            groupId = groupId,
            memberUserId = memberUserId,
            preselectedDeviceId = preselectedDeviceId
        )
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun addPreviousMonth() {
        val currentState = _uiState.value

        val updatedDays = historyDateMapper.createDatesForPreviousMonth(
            selectedDate = currentState.selectedDate,
            dateRangeDays = currentState.daySelectorState.dateRangeDays,
            currentDays = currentState.daySelectorState.days
        )

        if (updatedDays != null) {
            _uiState.update {
                it.copy(daySelectorState = it.daySelectorState.copy(days = updatedDays))
            }
        }
    }

    fun handleDayLongCLick(
        selectedDate: LocalDate
    ) {
        if (_uiState.value.isLoadingLocations) {
            return
        }

        val currentState = _uiState.value

        if (!currentState.canSelectDate(selectedDate)) {
            messageDispatcher.showSnackbar(text = MR.strings.history_map_date_unselectable_error.localizedString())
            return
        }

        // If currently a single day is selected
        if (currentState.startDate == currentState.endDate) {
            // If clicking the same day, do nothing
            if (currentState.selectedDate == selectedDate) {
                handleDaySelection(
                    dateRangeDays = currentState.daySelectorState.dateRangeDays ?: 1,
                    selectedDate = selectedDate,
                    forceReload = true
                )
                return
            }

            // Create initial range
            val difference = currentState.selectedDate.daysUntil(selectedDate)
            if (difference < 0) {
                // newly selected date is earlier than current selection
                if (difference.absoluteValue >= LocationHistoryScreenState.maxDateRangeDays) {
                    handleDaySelection(
                        dateRangeDays = LocationHistoryScreenState.maxDateRangeDays,
                        selectedDate = selectedDate.plus(DatePeriod(days = LocationHistoryScreenState.maxDateRangeDays - 1))
                    )
                } else {
                    handleDaySelection(
                        dateRangeDays = difference.absoluteValue + 1,
                        selectedDate = currentState.selectedDate
                    )
                }
            } else {
                // newly selected date is after the current selection
                if (difference >= LocationHistoryScreenState.maxDateRangeDays) {
                    handleDaySelection(
                        dateRangeDays = LocationHistoryScreenState.maxDateRangeDays,
                        selectedDate = selectedDate
                    )
                } else {
                    handleDaySelection(
                        dateRangeDays = difference + 1,
                        selectedDate = selectedDate
                    )
                }
            }
        } else {
            // Range is already selected - extend or collapse
            val startDate = currentState.startDate
            val endDate = currentState.endDate

            when {
                selectedDate < startDate -> {
                    // Extend backward (earlier in time)
                    val daysToEnd = selectedDate.daysUntil(endDate) + 1
                    if (daysToEnd > LocationHistoryScreenState.maxDateRangeDays) {
                        // Cap at max range, keep end date
                        handleDaySelection(
                            dateRangeDays = LocationHistoryScreenState.maxDateRangeDays,
                            selectedDate = selectedDate.plus(DatePeriod(days = LocationHistoryScreenState.maxDateRangeDays - 1))
                        )
                    } else {
                        handleDaySelection(
                            dateRangeDays = daysToEnd,
                            selectedDate = endDate
                        )
                    }
                }

                selectedDate > endDate -> {
                    // Extend forward (later in time)
                    val daysFromStart = startDate.daysUntil(selectedDate) + 1
                    if (daysFromStart > LocationHistoryScreenState.maxDateRangeDays) {
                        // Cap at max range, move end date forward
                        handleDaySelection(
                            dateRangeDays = LocationHistoryScreenState.maxDateRangeDays,
                            selectedDate = selectedDate
                        )
                    } else {
                        handleDaySelection(
                            dateRangeDays = daysFromStart,
                            selectedDate = selectedDate
                        )
                    }
                }

                selectedDate == endDate -> {
                    // tapping same selected date, force reload
                    handleDaySelection(
                        dateRangeDays = currentState.daySelectorState.dateRangeDays ?: 1,
                        selectedDate = selectedDate,
                        forceReload = true
                    )
                }

                else -> {
                    // Click within current range, collapse to single day
                    handleDaySelection(
                        dateRangeDays = 1,
                        selectedDate = selectedDate
                    )
                }
            }
        }
    }

    fun handleDaySelection(
        dateRangeDays: Int,
        selectedDate: LocalDate,
        forceReload: Boolean = false
    ) {
        if (_uiState.value.isLoadingLocations) {
            return
        }

        val currentState = _uiState.value

        if (!currentState.canSelectDate(selectedDate)) {
            messageDispatcher.showSnackbar(text = MR.strings.history_map_date_unselectable_error.localizedString())
            return
        }

        // Calculate new start and end dates based on selected day and range
        val newStartDate = when {
            dateRangeDays <= 1 -> selectedDate
            else -> selectedDate.minus(DatePeriod(days = dateRangeDays - 1))
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
            val daysInLoading = state.daySelectorState.daysInLoading
                .plus(updatedDays.filter { it.isSelected }.map { it.date })
                .distinct()
            state.copy(
                daySelectorState = state.daySelectorState.copy(
                    days = updatedDays,
                    daysInLoading = daysInLoading,
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
            endDate = _uiState.value.endDate,
            forceReload = forceReload
        )
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun selectDevice(device: UserActiveLocationDeviceState) {
        if (_uiState.value.isLoadingLocations) {
            return
        }

        _uiState.update {
            val newState = it.copy(
                devices = it.activeDevices.map {
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
                    coroutineScope.launch {
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
                            messageDispatcher.showSnackbar(e.errorString())
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
                    coroutineScope.launch {
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
                            messageDispatcher.showSnackbar(e.errorString())
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