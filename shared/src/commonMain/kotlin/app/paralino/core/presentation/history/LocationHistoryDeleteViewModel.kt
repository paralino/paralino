package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.DeleteLocationHistoryPeriodUseCase
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.dialogs.MainAlertDialog.DeleteLocationHistoryDays
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
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
    private val groupId: GroupOrSpaceId,
    private val userRepo: UserRepository,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userGroupRepo: UserGroupLocalDataSource,
    private val deleteHistoryUseCase: DeleteLocationHistoryPeriodUseCase,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<LocationHistoryDeleteScreenState>(
        LocationHistoryDeleteScreenState(
            groupId = groupId,
            groupName = "",
            groupImage = ImageModel.emoji(Emojis.lock),
            isContentLoading = true,
            isDeleteLoading = null,
            hasDeviceSelectionError = false,
            devices = emptyList(),
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
            if (groupId.isPersonalSpace) {
                // Personal history
                personalSpaceRepo.personalSpaceState
                    .mapNotNull { it.data }
                    .onEach { space ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                groupId = groupId,
                                groupName = MR.strings.personalSpace_spaceTitle.localizedString(),
                                groupImage = space.user.data?.userImage ?: ImageModel.emoji(Emojis.robot),
                                isContentLoading = false,
                                devices = space.devices.map { device ->
                                    LocationHistoryDeleteDeviceState(
                                        deviceId = device.deviceId,
                                        locationId = device.id,
                                        deviceName = device.deviceNameOrNull ?: MR.strings.history_map_device_name_unknown.localizedString(),
                                        tag = device.deviceType?.toString(),
                                        isSelected = currentState.devices.firstOrNull { it.locationId == device.id }?.isSelected ?: false,
                                        isCurrentDevice = device.deviceId == appPrefs.deviceId,
                                    )
                                }
                            )
                        }
                    }
                    .launchIn(coroutineScope)
            } else {
                // Group history
                combine(
                    userGroupRepo.getGroupWithMembers(groupId.id)
                        .filterNotNull(),
                    userRepo.localUser
                ) { group, user ->
                    val currentMember = group.currentUserMemberWithLocations!!
                    _uiState.update { currentState ->
                        val devices = currentMember.locations.map { location ->
                            LocationHistoryDeleteDeviceState(
                                deviceId = location.deviceId,
                                locationId = location.id,
                                deviceName = location.deviceData.data?.deviceName ?: MR.strings.history_map_device_name_unknown.localizedString(),
                                tag = location.deviceData.data?.deviceType?.toString(),
                                isSelected = currentState.devices.firstOrNull { it.locationId == location.id }?.isSelected ?: false,
                                isCurrentDevice = location.deviceId == appPrefs.deviceId,
                            )
                        }
                        currentState.copy(
                            groupId = groupId,
                            groupName = group.name,
                            groupImage = group.image,
                            isContentLoading = false,
                            devices = devices
                        )
                    }
                }
                    .launchIn(coroutineScope)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isContentLoading = true)
            }
            showSnackbar(e.errorString())
        }
    }

    fun onEvent(event: LocationHistoryDeleteUiEvent) {
        if (_uiState.value.isDeleteLoading == true) {
            return
        }

        when (event) {
            is LocationHistoryDeleteUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }
                when (event.action.dialog) {
                    is MainAlertDialog.DeleteLocationHistoryDays -> {
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            confirmDeletion()
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
                if (!_uiState.value.isDeletingEverything && _uiState.value.devices.none { it.isSelected }) {
                    _uiState.update {
                        it.copy(hasDeviceSelectionError = true)
                    }
                    return
                } else if (uiState.value.fromDate.isEnabled || uiState.value.untilDate.isEnabled) {
                    _uiState.update {
                        it.copy(
                            hasDeviceSelectionError = false,
                            alertDialog = DeleteLocationHistoryDays(content = it.deleteInfoContent ?: "")
                        )
                    }
                }
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

            is LocationHistoryDeleteUiEvent.ToggleDeviceSelection -> {
                toggleDeviceSelection(selectedDevice = event.device)
            }
        }
    }

    private fun confirmDeletion() {
        _uiState.update { it.copy(isDeleteLoading = true) }
        coroutineScope.launch {
            try {
                deleteHistoryUseCase.deleteHistory(
                    groupId = groupId,
                    locationIds = _uiState.value.devices.filter { it.isSelected }.map { it.locationId },
                    deleteForAllDevices = _uiState.value.isDeletingEverything,
                    startDate = uiState.value.fromDate.date.takeIf { uiState.value.fromDate.isEnabled },
                    endDate = uiState.value.untilDate.date.takeIf { uiState.value.untilDate.isEnabled },
                )
                _uiState.update { it.copy(isDeleteLoading = false) }
                delay(300)
                _eventChannel.trySend(LocationHistoryDeleteScreenEvent.NavigateBack)
            } catch (e: Exception) {
                showSnackbar(e.errorString())
                _uiState.update { it.copy(isDeleteLoading =  null) }
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
            formattedFullDate = valueFormatter.formatBestPatternLocal(
                timeLocal = selectedDate.atTime(0, 0),
                pattern = "MMMMdyyyy"
            ),
            formattedMonth = valueFormatter.formatWithPattern(
                timeUTC = selectedDate.atTime(0, 0),
                pattern = "MMMM"
            ),
            days = days
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
                    isLoading = false,
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

    private fun updateYearMonth(dateState: LocationHistoryDeleteDateState, yearOffset: Int, monthOffset: Int): LocationHistoryDeleteDateState {
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

    private fun navigateStartDateYear(forward: Boolean) {
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

    private fun navigateStartDateMonth(forward: Boolean) {
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

    private fun navigateEndDateYear(forward: Boolean) {
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

    private fun navigateEndDateMonth(forward: Boolean) {
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

    private fun toggleDeviceSelection(selectedDevice: LocationHistoryDeleteDeviceState) {
        _uiState.update { state ->
            state.copy(
                hasDeviceSelectionError = false,
                devices = state.devices.map { device ->
                    if (device.locationId == selectedDevice.locationId) {
                        device.copy(isSelected = !device.isSelected)
                    } else {
                        device
                    }
                }
            )
        }
    }
}