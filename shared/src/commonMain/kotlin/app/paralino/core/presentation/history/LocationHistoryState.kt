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
    val stats: LocationHistoryStats?,
    val mapFocusType: LocationHistoryMapFocusType,
    val selectedLocationIndex: Int?,
    val isSatelliteMode: Boolean = false,
    val alertDialog: MainAlertDialog? = null,
) {
    val deviceType: DeviceType = DeviceType.current
    val selectedLocations = locations.filter { entry ->
        devices.any { device ->
            when (device) {
                is UserActiveLocationDeviceState.Known -> {
                    device.isSelected && device.locationId == entry.locationId
                }

                is UserActiveLocationDeviceState.Unknown -> {
                    device.isSelected && (device.ids.contains(entry.locationId) || entry.locationId == null)
                }
            }
        }
    }.reversed()

    val locationIndex: Int = selectedLocationIndex?.takeIf {
        it >= 0 && it < selectedLocations.size
    } ?: selectedLocations.lastIndex

    val selectedPoint: LocationHistoryPointState? = if (mapFocusType is LocationHistoryMapFocusType.LocationPoint) {
        selectedLocations.withIndex().firstOrNull {
            it.index == locationIndex
        }?.value ?: selectedLocations.firstOrNull()
    } else {
        null
    }

    val selectedMapPinData: MapPinData? = selectedPoint?.point?.loc?.let {
        MapPinData(
            groupId = groupId,
            location = it,
            name = userName,
            image = userImage,
            timeInfo = null
        )
    }
}

data class LocationHistoryStats(
    val formattedTime: String?,
    val totalDistanceMeters: Float,
    val batteryRange: Pair<Int, Int>,
    val averageBattery: Int,
    val maxSpeedMps: Float,
    val elevationRangeMeters: Pair<Float, Float>,
    val elevationGainMeters: Float,
    val locationEntriesCount: Int,
    val networks: List<String>,

    val formattedDistance: String,
    val formattedMaxSpeed: String,
    val formattedElevationRange: String,
    val formattedElevationGain: String
)

sealed class UserActiveLocationDeviceState {
    abstract val locationId: String
    abstract val deviceId: String
    abstract val deviceName: String
    abstract val isSelected: Boolean

    data class Known(
        override val locationId: String,
        override val deviceId: String,
        override val deviceName: String,
        override val isSelected: Boolean
    ) : UserActiveLocationDeviceState()

    data class Unknown(
        val ids: List<String?>,
        override val deviceName: String,
        override val isSelected: Boolean
    ) : UserActiveLocationDeviceState() {
        override val deviceId: String = "unknown"
        override val locationId: String = "unknown"
    }
}

data class LocationHistoryPointState(
    val locationId: String?,
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
    data object Stats : LocationHistoryMapFocusType
    data object Timeline : LocationHistoryMapFocusType
    data object LocationPoint: LocationHistoryMapFocusType

    val isLocationPoint: Boolean get() = this is LocationPoint
}

sealed interface LocationHistoryUiEvent {
    data class UiLifecycleChange(val active: Boolean) : LocationHistoryUiEvent
    data class SelectLocation(val index: Int) : LocationHistoryUiEvent
    data class SelectDevice(val device: UserActiveLocationDeviceState) : LocationHistoryUiEvent
    data class DialogAction(val action: AlertDialogAction) : LocationHistoryUiEvent
    data class OnMarkerClick(val pin: MapPinData) : LocationHistoryUiEvent
    data class OnMapLongPress(val lat: Double, val lng: Double) : LocationHistoryUiEvent
    data class SetHistoryProgress(val progress: Int) : LocationHistoryUiEvent
    data class ChangeMapFocusType(val type: LocationHistoryMapFocusType) : LocationHistoryUiEvent
    data object OnRefocusClick : LocationHistoryUiEvent
    data object OnMapModeSwitch : LocationHistoryUiEvent
    data object OnCloseFocusedItemClick : LocationHistoryUiEvent
    data object OnMapTouch : LocationHistoryUiEvent
    data object OnPreviousDay : LocationHistoryUiEvent
    data object OnNextDay : LocationHistoryUiEvent
    data object ToggleDayRange : LocationHistoryUiEvent
    data object OnCloseScreen : LocationHistoryUiEvent
}

sealed interface LocationHistoryScreenEvent {
    data class Error(val error: Throwable) : LocationHistoryScreenEvent
    data object NavigateBack : LocationHistoryScreenEvent
}