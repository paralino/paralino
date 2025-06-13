package app.paralino.core.presentation.history

import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.current
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.datetime.LocalDate

data class LocationHistoryScreenState(
    val groupId: String,
    val memberUserId: String,
    val groupName: String?,
    val groupImage: ImageModel?,
    val historyDaysLimit: Int?,
    val isGroupOwner: Boolean,
    val userName: String?,
    val userImage: ImageModel?,
    val deviceName: String?,
    val isCurrentUser: Boolean,
    val isPersonalHistory: Boolean,
    val dateRangeDays: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isDataMissing: Boolean,
    val isLoadingData: Boolean,
    val isLoadingLocations: Boolean,
    val devices: List<UserActiveLocationDeviceState>,
    val locations: List<LocationHistoryPointState>,
    val selectedLocationId: String?,
    val selectedLocationIndex: Int?,
    val mapFocusType: LocationHistoryMapFocusType,
    val isSatelliteMode: Boolean = false,
    val alertDialog: MainAlertDialog? = null,
) {
    val deviceType: DeviceType = DeviceType.current
}

data class UserActiveLocationDeviceState(
    val locationId: String,
    val deviceId: String,
    val deviceName: String
)

data class LocationHistoryPointState(
    val point: LocationHistoryPointModel,
    val formattedSpeed: String?,
    val formattedElevation: String?,
    val locationTime: String?,
    val statusTime: String?,
    val deviceStatus: MemberDeviceStatusType?,
    val networkType: NetworkType?,
    val signalStrength: NetworkSignalStrength?
)

sealed interface LocationHistoryMapFocusType {
    data class LocationPoint(val state: LocationHistoryPointState) : LocationHistoryMapFocusType
    data object None : LocationHistoryMapFocusType

    val isLocationPoint: Boolean get() = this is LocationPoint
}

sealed interface LocationHistoryUiEvent {
    data class UiLifecycleChange(val active: Boolean) : LocationHistoryUiEvent
    data class SelectLocation(val index: Int) : LocationHistoryUiEvent
    data class SelectDevice(val locationId: String) : LocationHistoryUiEvent
    data class DialogAction(val action: AlertDialogAction) : LocationHistoryUiEvent
    data class OnMarkerClick(val pin: MapPinData) : LocationHistoryUiEvent
    data class OnMapLongPress(val lat: Double, val lng: Double) : LocationHistoryUiEvent
    data class SetHistoryProgress(val progress: Int) : LocationHistoryUiEvent
    data object OnRefocusClick : LocationHistoryUiEvent
    data object OnMapModeSwitch : LocationHistoryUiEvent
    data object OnCloseFocusedItemClick : LocationHistoryUiEvent
    data object OnMapTouch : LocationHistoryUiEvent
    data object OnPreviousDay : LocationHistoryUiEvent
    data object OnNextDay : LocationHistoryUiEvent
    data object ToggleDayRange : LocationHistoryUiEvent
}

sealed interface LocationHistoryScreenEvent {
    data class Error(val error: Throwable) : LocationHistoryScreenEvent
}