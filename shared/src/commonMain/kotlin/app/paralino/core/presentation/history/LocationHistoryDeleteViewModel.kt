package app.paralino.core.presentation.history

import app.paralino.core.domain.location.history.DeleteLocationHistoryPeriodUseCase
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class LocationHistoryDeleteViewModel(
    private val groupId: String,
    private val userRepo: UserRepository,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val deleteHistoryUseCase: DeleteLocationHistoryPeriodUseCase,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<LocationHistoryDeleteScreenState>(
        LocationHistoryDeleteScreenState(
            groupId = "",
            groupName = "",
            groupImage = ImageModel.emoji(Emojis.lock),
            isContentLoading = true,
            isDeleteLoading = null,
            fromDate = createDateState(
                isEnabled = false,
                selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            ),
            untilDate = createDateState(
                isEnabled = false,
                selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            ),
            alertDialog = null
        )
    )
    val uiState: StateFlow<LocationHistoryDeleteScreenState> = _uiState

    private val _eventChannel = Channel<LocationHistoryDeleteScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _uiState.update { it.copy(isContentLoading = true) }

        try {
            if (groupId == UserGroup.groupPersonalDevicesId) {
                // Personal history
                userRepo.localUser
                    .onEach { user ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                groupId = groupId,
                                groupName = "Personal History",
                                groupImage = user.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
                                isContentLoading = false,
                            )
                        }
                    }
                    .launchIn(coroutineScope)
            } else {
                // Group history
                combine(
                    userGroupRepo.getGroupWithoutMembers(groupId),
                    userRepo.localUser
                ) { group, user ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            groupId = groupId,
                            groupName = group.name,
                            groupImage = group.image,
                            isContentLoading = false,
                        )
                    }
                }
                    .launchIn(coroutineScope)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isContentLoading = true)
            }
            showSnackbar(e.toString())
        }
    }

    fun onEvent(event: LocationHistoryDeleteUiEvent) {
        when (event) {
            is LocationHistoryDeleteUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }

                when (event.action.dialog) {
                    is MainAlertDialog.DeleteLocationHistoryDays -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            _uiState.update { it.copy(isDeleteLoading = true) }
                            // For now, just simulate deletion and navigate back
                            coroutineScope.launch {
                                try {
                                    deleteHistoryUseCase.deleteHistory(
                                        groupId = groupId,
                                        startDate = uiState.value.fromDate.date.takeIf { uiState.value.fromDate.isEnabled },
                                        endDate = uiState.value.untilDate.date.takeIf { uiState.value.untilDate.isEnabled },
                                    )
                                    _uiState.update { it.copy(isDeleteLoading = false) }
                                    delay(400)
                                    _eventChannel.trySend(LocationHistoryDeleteScreenEvent.NavigateBack)
                                } catch (e: Exception) {
                                    showSnackbar(e.toString())
                                    _uiState.update { it.copy(isDeleteLoading =  null) }
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }

            LocationHistoryDeleteUiEvent.ToggleStartDate -> {
                _uiState.update {
                    it.copy(fromDate = it.fromDate.copy(isEnabled = !it.fromDate.isEnabled))
                }
            }

            LocationHistoryDeleteUiEvent.ToggleEndDate -> {
                _uiState.update {
                    it.copy(untilDate = it.untilDate.copy(isEnabled = !it.untilDate.isEnabled))
                }
            }

            is LocationHistoryDeleteUiEvent.OnSelectStartDay -> {
                updateStartDate(event.day)
            }

            is LocationHistoryDeleteUiEvent.OnSelectEndDay -> {
                updateEndDate(event.day)
            }

            LocationHistoryDeleteUiEvent.OnDeleteAction -> {
                if (uiState.value.fromDate.isEnabled || uiState.value.untilDate.isEnabled) {
                    _uiState.update {
                        it.copy(
                            alertDialog = MainAlertDialog.DeleteLocationHistoryDays(content = it.deleteInfoContent ?: "")
                        )
                    }
                }
            }

            LocationHistoryDeleteUiEvent.OnCloseScreen -> {
                _eventChannel.trySend(LocationHistoryDeleteScreenEvent.NavigateBack)
            }

            LocationHistoryDeleteUiEvent.IncreaseStartYear -> {
                navigateStartDateYear(forward = true)
            }
            LocationHistoryDeleteUiEvent.DecreaseStartYear -> {
                navigateStartDateYear(forward = false)
            }
            LocationHistoryDeleteUiEvent.IncreaseStartMonth -> {
                navigateStartDateMonth(forward = true)
            }
            LocationHistoryDeleteUiEvent.DecreaseStartMonth -> {
                navigateStartDateMonth(forward = false)
            }
            LocationHistoryDeleteUiEvent.IncreaseEndYear -> {
                navigateEndDateYear(forward = true)
            }
            LocationHistoryDeleteUiEvent.DecreaseEndYear -> {
                navigateEndDateYear(forward = false)
            }
            LocationHistoryDeleteUiEvent.IncreaseEndMonth -> {
                navigateEndDateMonth(forward = true)
            }
            LocationHistoryDeleteUiEvent.DecreaseEndMonth -> {
                navigateEndDateMonth(forward = false)
            }
        }
    }

    private fun updateStartDate(selectedDate: LocalDate) {
        val currentState = _uiState.value
        val newFromDate = createDateState(isEnabled = true, selectedDate)
        
        // If new start date is after current end date, adjust end date to match start date
        val adjustedUntilDate = if (selectedDate > currentState.untilDate.date) {
            createDateState(isEnabled = currentState.untilDate.isEnabled, selectedDate)
        } else {
            currentState.untilDate
        }

        _uiState.update { 
            it.copy(
                fromDate = newFromDate,
                untilDate = adjustedUntilDate
            ) 
        }
    }

    private fun updateEndDate(selectedDate: LocalDate) {
        val currentState = _uiState.value
        val newUntilDate = createDateState(isEnabled = true, selectedDate)
        
        // If new end date is before current start date, adjust start date to match end date
        val adjustedFromDate = if (selectedDate < currentState.fromDate.date) {
            createDateState(isEnabled = currentState.fromDate.isEnabled, selectedDate)
        } else {
            currentState.fromDate
        }

        _uiState.update { 
            it.copy(
                fromDate = adjustedFromDate,
                untilDate = newUntilDate
            ) 
        }
    }

    private fun createDateState(isEnabled: Boolean, selectedDate: LocalDate): LocationHistoryDeleteDateState {
        val days = generateDaysForMonth(selectedDate)
        return LocationHistoryDeleteDateState(
            isEnabled = isEnabled,
            date = selectedDate,
            formattedFullDate = valueFormatter.formatBestPattern(
                timeUTC = selectedDate.atTime(0, 0),
                pattern = "MMMMdyyyy"
            ),
            formattedMonth = valueFormatter.formatWithPattern(
                timeUTC = selectedDate.atTime(0, 0),
                pattern = "MMMM"
            ),
            days = days.reversed()
        )
    }

    private fun generateDaysForMonth(selectedDate: LocalDate): List<LocationHistoryDayState> {
        val days = mutableListOf<LocationHistoryDayState>()
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Generate days for the selected month
        val firstDayOfMonth = LocalDate(selectedDate.year, selectedDate.month, 1)
        val lastDayOfMonth = firstDayOfMonth.plus(DatePeriod(months = 1)).plus(DatePeriod(days = -1))

        var currentDate = firstDayOfMonth
        while (currentDate <= lastDayOfMonth && currentDate <= today) {
            days.add(
                LocationHistoryDayState(
                    isSelected = currentDate == selectedDate,
                    date = currentDate,
                    formattedMonth = null,
                    formattedDay = valueFormatter.formatWithPattern(
                        timeUTC = currentDate.atTime(0, 0),
                        pattern = "EEE"
                    ),
                    isWeekend = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY,
                    position = LocationHistorySelectedDayPosition.SOLO
                )
            )
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        return days
    }

    fun updateYearMonth(dateState: LocationHistoryDeleteDateState, yearOffset: Int, monthOffset: Int): LocationHistoryDeleteDateState {
        val newDate = if (yearOffset != 0) {
            LocalDate(dateState.year + yearOffset, dateState.date.month, 1)
        } else {
            val currentMonthNumber = dateState.date.monthNumber
            val newMonthNumber = currentMonthNumber + monthOffset
            val newYear = when {
                newMonthNumber <= 0 -> dateState.year - 1
                newMonthNumber > 12 -> dateState.year + 1
                else -> dateState.year
            }
            
            val adjustedMonth = when {
                newMonthNumber <= 0 -> 12 + newMonthNumber
                newMonthNumber > 12 -> newMonthNumber - 12
                else -> newMonthNumber
            }
            
            LocalDate(newYear, adjustedMonth, 1)
        }
        
        // Ensure we don't go to future dates
        val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val clampedDate = if (newDate > today) today else newDate
        
        return createDateState(isEnabled = dateState.isEnabled, clampedDate)
    }

    fun navigateStartDateYear(forward: Boolean) {
        val currentState = _uiState.value
        val offset = if (forward) 1 else -1
        val newFromDateState = updateYearMonth(currentState.fromDate, offset, 0)
        
        // If new start date is after current end date, adjust end date to match start date
        val adjustedUntilDate = if (newFromDateState.date > currentState.untilDate.date) {
            createDateState(isEnabled = currentState.untilDate.isEnabled, newFromDateState.date)
        } else {
            currentState.untilDate
        }

        _uiState.update { 
            it.copy(
                fromDate = newFromDateState,
                untilDate = adjustedUntilDate,
            )
        }
    }

    fun navigateStartDateMonth(forward: Boolean) {
        val currentState = _uiState.value
        val offset = if (forward) 1 else -1
        val newFromDateState = updateYearMonth(currentState.fromDate, 0, offset)
        
        // If new start date is after current end date, adjust end date to match start date
        val adjustedUntilDate = if (newFromDateState.date > currentState.untilDate.date) {
            createDateState(isEnabled = currentState.untilDate.isEnabled, newFromDateState.date)
        } else {
            currentState.untilDate
        }
        
        _uiState.update {
            it.copy(
                fromDate = newFromDateState,
                untilDate = adjustedUntilDate,
            )
        }
    }

    fun navigateEndDateYear(forward: Boolean) {
        val currentState = _uiState.value
        val offset = if (forward) 1 else -1
        val newUntilDateState = updateYearMonth(currentState.untilDate, offset, 0)
        
        // If new end date is before current start date, adjust start date to match end date
        val adjustedFromDate = if (newUntilDateState.date < currentState.fromDate.date) {
            createDateState(isEnabled = currentState.fromDate.isEnabled, newUntilDateState.date)
        } else {
            currentState.fromDate
        }
        
        _uiState.update {
            it.copy(
                fromDate = adjustedFromDate,
                untilDate = newUntilDateState,
            )
        }
    }

    fun navigateEndDateMonth(forward: Boolean) {
        val currentState = _uiState.value
        val offset = if (forward) 1 else -1
        val newUntilDateState = updateYearMonth(currentState.untilDate, 0, offset)
        
        // If new end date is before current start date, adjust start date to match end date
        val adjustedFromDate = if (newUntilDateState.date < currentState.fromDate.date) {
            createDateState(isEnabled = currentState.fromDate.isEnabled, newUntilDateState.date)
        } else {
            currentState.fromDate
        }
        
        _uiState.update {
            it.copy(
                fromDate = adjustedFromDate,
                untilDate = newUntilDateState,
            )
        }
    }
}