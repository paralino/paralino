package app.paralino.core.presentation.history

import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberNavigateItemState
import app.paralino.core.presentation.map.MapHomeScreenEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.current
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.min

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
    val selectedDate: LocalDate,
    val isDeletingEntry: Boolean,
    val deletionRangeStartIndex: Int?,
    val isRefreshingData: Boolean,
    val isLoadingLocations: Boolean,
    val showPolyline: Boolean,
    val showPins: Boolean,
    val clusterPins: Boolean,
    val isFilterOpened: Boolean,
    val devices: List<UserActiveLocationDeviceState>,
    val locations: List<LocationHistoryPointState>,
    val viewMode: LocationHistoryPathViewMode,
    val stats: LocationHistoryStats?,
    val dataViewType: LocationHistoryDataViewType,
    val selectedLocationId: String?,
    val days: List<LocationHistoryDayState>,
    val daysInLoading: List<LocalDate>,
    val isSatelliteMode: Boolean = false,
    val alertDialog: MainAlertDialog? = null,
) {
    val isDataMissing: Boolean = groupId.isBlank() || memberUserId.isBlank()

    val startDate: LocalDate = selectedDate.minus(DatePeriod(days = dateRangeDays - 1))
    val endDate: LocalDate = selectedDate
    val displayedDays = days.reversed().map { it.copy(isLoading = daysInLoading.contains(it.date)) }

    val deviceType: DeviceType = DeviceType.current
    private val displayLocations = when (viewMode) {
        LocationHistoryPathViewMode.OVERVIEW -> {
            locations
        }

        LocationHistoryPathViewMode.DETAILED -> {
            locations.flatMap {
                when (it) {
                    is LocationHistoryPointState.Cluster -> it.points
                    is LocationHistoryPointState.Point -> listOf(it)
                }
            }
        }
    }

    val selectedLocations = displayLocations.filter { entry ->
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
    }

    val locationIndex: Int = selectedLocationId?.let { id ->
        selectedLocations.indexOfFirst {
            when (it) {
                is LocationHistoryPointState.Cluster -> it.points.any { it.point.properID == id }
                is LocationHistoryPointState.Point -> it.point.properID == id
            }
        }.takeIf { it >= 0 }
    } ?: selectedLocations.lastIndex

    val selectedPoint: LocationHistoryPointState? =
        if (dataViewType is LocationHistoryDataViewType.LocationPoint) {
            selectedLocations.withIndex().firstOrNull {
                it.index == locationIndex
            }?.value ?: selectedLocations.firstOrNull()
        } else {
            null
        }

    val selectedMapPinData: MapPinData? = selectedPoint?.let {
        when (it) {
            is LocationHistoryPointState.Cluster -> {
                MapPinData(
                    id = memberUserId,
                    groupId = groupId,
                    cluster = it,
                    name = userName,
                    image = userImage,
                    timeInfo = null
                )
            }
            is LocationHistoryPointState.Point -> {
                it.point.loc?.let {
                    MapPinData(
                        id = memberUserId,
                        groupId = groupId,
                        location = it,
                        name = userName,
                        image = userImage,
                        timeInfo = null
                    )
                }
            }
        }
    }

    val deleteAction =
        if (
            isCurrentUser &&
            selectedPoint != null &&
            (
                    devices.firstOrNull { it.locationId == selectedPoint.locationId }?.isCurrent == true ||
                            (selectedPoint is LocationHistoryPointState.Point && !selectedPoint.isCachedLocation))
            )
        {
            MemberActionItemState(
                type = MemberActionType.DELETE_LOCATION,
                isLoading = isDeletingEntry
            )
        } else {
            null
        }

    val navigateAction = selectedMapPinData?.let {
        MemberNavigateItemState(
            lat = it.lat,
            lng = it.lng,
            title = it.title
        )
    }

    val locationsToDelete = if (deletionRangeStartIndex != null && selectedLocationId != null) {
        val rangeStart = min(deletionRangeStartIndex, locationIndex)
        val rangeEnd = max(deletionRangeStartIndex, locationIndex)
        selectedLocations.subList(
            fromIndex = rangeStart,
            toIndex = rangeEnd + 1
        ).flatMap {
            when (it) {
                is LocationHistoryPointState.Cluster -> it.points
                is LocationHistoryPointState.Point -> listOf(it)
            }
        }
    } else {
        emptyList()
    }

    companion object {
        fun emptyState(): LocationHistoryScreenState {
            val selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return LocationHistoryScreenState(
                groupId = "",
                memberUserId = "",
                groupName = null,
                groupImage = null,
                userName = null,
                userImage = null,
                isCurrentUser = false,
                isPersonalHistory = false,
                historyDaysLimit = null,
                isGroupOwner = false,
                dateRangeDays = 1,
                selectedDate = selectedDate,
                isDeletingEntry = false,
                deletionRangeStartIndex = null,
                isRefreshingData = true,
                isLoadingLocations = true,
                showPolyline = true,
                showPins = true,
                clusterPins = true,
                isFilterOpened = false,
                locations = emptyList(),
                viewMode = LocationHistoryPathViewMode.OVERVIEW,
                devices = emptyList(),
                stats = null,
                dataViewType = LocationHistoryDataViewType.Stats,
                selectedLocationId = null,
                days = emptyList(),
                daysInLoading = emptyList(),
                isSatelliteMode = false,
                alertDialog = null
            )
        }
    }
}

sealed class UserActiveLocationDeviceState {
    abstract val locationId: String
    abstract val deviceId: String
    abstract val deviceName: String
    abstract val isSelected: Boolean
    abstract val isCurrent: Boolean

    data class Known(
        override val locationId: String,
        override val deviceId: String,
        override val deviceName: String,
        override val isSelected: Boolean,
        override val isCurrent: Boolean
    ) : UserActiveLocationDeviceState()

    data class Unknown(
        val ids: List<String?>,
        override val deviceName: String,
        override val isSelected: Boolean
    ) : UserActiveLocationDeviceState() {
        override val deviceId: String = "unknown"
        override val locationId: String = "unknown"
        override val isCurrent: Boolean = false
    }
}

sealed interface LocationHistoryPointState {
    val pointId: String
    val locationId: String?
    val locationTime: String?
    val statusTime: String?
    val formattedSpeed: String?
    val formattedElevation: String?
    val lat: Double?
    val lng: Double?
    val accuracy: Float?

    data class Cluster(
        override val locationId: String?,
        override val locationTime: String?,
        val points: List<LocationHistoryPointState.Point>,
        override val lat: Double,
        override val lng: Double,
        override val accuracy: Float,
        val battery: String?
    ): LocationHistoryPointState {
        override val statusTime: String? = null
        override val formattedElevation: String? = null
        override val formattedSpeed: String? = null
        override val pointId: String = points.first().point.properID
    }

    data class Point(
        override val locationId: String?,
        override val formattedSpeed: String?,
        override val formattedElevation: String?,
        val dateKey: String,
        val entryId: String,
        val isCachedLocation: Boolean,
        val point: LocationHistoryPointModel,
        override val locationTime: String?,
        override val statusTime: String? = null,
        val deviceStatus: MemberDeviceStatusType?,
        val networkType: NetworkType?,
        val signalStrength: NetworkSignalStrength?
    ): LocationHistoryPointState {
        override val pointId: String = point.properID
        override val lat: Double? = point.loc?.lat
        override val lng: Double? = point.loc?.lng
        override val accuracy: Float? = point.loc?.acc
    }
}

sealed interface LocationHistoryDataViewType {
    data object Stats : LocationHistoryDataViewType
    data object LocationPoint : LocationHistoryDataViewType
}

enum class LocationHistoryPathViewMode {
    OVERVIEW,
    DETAILED
}

data class LocationHistoryDayState(
    val isSelected: Boolean,
    val isLoading: Boolean,
    val isWeekend: Boolean,
    val date: LocalDate,
    val formattedDay: String,
    val formattedMonth: String?,
    val position: LocationHistorySelectedDayPosition
) {
    val year = date.year
    val day = date.dayOfMonth
}

enum class LocationHistorySelectedDayPosition {
    START, CENTER, END, SOLO;
}

sealed interface LocationHistoryUiEvent {
    data class LoadHistory(val groupId: String, val memberUserId: String) : LocationHistoryUiEvent
    data class UiLifecycleChange(val active: Boolean) : LocationHistoryUiEvent
    data class SelectDevice(val device: UserActiveLocationDeviceState) : LocationHistoryUiEvent
    data class DialogAction(val action: AlertDialogAction) : LocationHistoryUiEvent
    data class OnMarkerClick(val pin: MapPinData) : LocationHistoryUiEvent
    data class OnPointClicked(val point: LocationHistoryPointState) : LocationHistoryUiEvent
    data class OnMapLongPress(val lat: Double, val lng: Double) : LocationHistoryUiEvent
    data class SetHistoryProgress(val progress: Int) : LocationHistoryUiEvent
    data class ChangeMapDataViewType(val type: LocationHistoryDataViewType) : LocationHistoryUiEvent
    data class OnDirectionsRequest(val action: MemberNavigateItemState) : LocationHistoryUiEvent
    data object ToggleClusterLocations : LocationHistoryUiEvent
    data object ToggleShowPins : LocationHistoryUiEvent
    data object ToggleShowPolyline : LocationHistoryUiEvent
    data object ToggleClusterPins : LocationHistoryUiEvent
    data object OnDeleteSelectedLocationRequest : LocationHistoryUiEvent
    data object OnRefocusClick : LocationHistoryUiEvent
    data object OnMapModeSwitch : LocationHistoryUiEvent
    data object OnCloseFocusedItemClick : LocationHistoryUiEvent
    data object OnMapTouch : LocationHistoryUiEvent
    data object ToggleMapFilter : LocationHistoryUiEvent
    data object OnPreviousMonth : LocationHistoryUiEvent
    data class OnSelectDay(val day: LocalDate) : LocationHistoryUiEvent
    data object ToggleDayRange : LocationHistoryUiEvent
    data object OnCloseScreen : LocationHistoryUiEvent
    data object OpenLocationSettings : LocationHistoryUiEvent
}

sealed interface LocationHistoryScreenEvent {
    data class OpenNavigation(
        val event: MapHomeScreenEvent.OpenExternalNavigation
    ) : LocationHistoryScreenEvent

    data object NavigateBack : LocationHistoryScreenEvent
    data object OpenHistorySettings : LocationHistoryScreenEvent
}