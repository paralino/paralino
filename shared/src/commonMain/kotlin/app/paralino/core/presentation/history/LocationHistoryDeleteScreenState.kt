package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.datetime.LocalDate

data class LocationHistoryDeleteScreenState(
    val groupId: String,
    val groupName: String,
    val groupImage: ImageModel,
    val isContentLoading: Boolean,
    val isDeleteLoading: Boolean?,
    val fromDate: LocationHistoryDeleteDateState,
    val untilDate: LocationHistoryDeleteDateState,
    val alertDialog: MainAlertDialog?,
) {
    val isDeleteButtonEnabled: Boolean = fromDate.isEnabled || untilDate.isEnabled
    val deleteInfoContent: String? get() {
        return when {
            !fromDate.isEnabled && !untilDate.isEnabled -> {
                null
            }

            // Only "From" date selected
            fromDate.isEnabled && !untilDate.isEnabled -> {
                MR.strings.history_delete_from.localizedString(fromDate.formattedFullDate)
            }

            // Only "Until" date selected
            !fromDate.isEnabled && untilDate.isEnabled -> {
                MR.strings.history_delete_until.localizedString(untilDate.formattedFullDate)
            }

            // Both dates selected but same date
            fromDate.date == untilDate.date -> {
                MR.strings.history_delete_on_date.localizedString(fromDate.formattedFullDate)
            }

            // Both dates selected and different (date range)
            else -> {
                MR.strings.history_delete_range_from_until.localizedString(fromDate.formattedFullDate, untilDate.formattedFullDate)
            }
        }
    }
}

data class LocationHistoryDeleteDateState(
    val isEnabled: Boolean,
    val date: LocalDate,
    val formattedFullDate: String,
    val formattedMonth: String,
    val days: List<LocationHistoryDayState>
) {
    val selectorState = LocationHistoryDateSelectorState(
        days = days,
        startAction = LocationHistoryDateSelectionAction.NONE,
        dateRangeDays = null,
        forceFocusOnSelectedDay = true
    )
    val year: Int = date.year
    val day = date.day
}

sealed interface LocationHistoryDeleteUiEvent {
    data class DialogAction(val action: AlertDialogAction) : LocationHistoryDeleteUiEvent
    data class OnSelectStartDay(val day: LocalDate) : LocationHistoryDeleteUiEvent
    data class OnSelectEndDay(val day: LocalDate) : LocationHistoryDeleteUiEvent

    data object IncreaseStartYear : LocationHistoryDeleteUiEvent
    data object DecreaseStartYear : LocationHistoryDeleteUiEvent
    data object IncreaseEndYear : LocationHistoryDeleteUiEvent
    data object DecreaseEndYear : LocationHistoryDeleteUiEvent

    data object IncreaseStartMonth : LocationHistoryDeleteUiEvent
    data object DecreaseStartMonth : LocationHistoryDeleteUiEvent
    data object IncreaseEndMonth : LocationHistoryDeleteUiEvent
    data object DecreaseEndMonth : LocationHistoryDeleteUiEvent

    data object ToggleStartDate : LocationHistoryDeleteUiEvent
    data object ToggleEndDate : LocationHistoryDeleteUiEvent
    data object OnDeleteAction : LocationHistoryDeleteUiEvent
    data object OnCloseScreen : LocationHistoryDeleteUiEvent
}

sealed interface LocationHistoryDeleteScreenEvent {
    data object NavigateBack : LocationHistoryDeleteScreenEvent
}