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
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.provider.current
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

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
    val isRefreshingData: Boolean,
    val isLoadingLocations: Boolean,
    val devices: List<UserActiveLocationDeviceState>,
    val locations: List<LocationHistoryPointState>,
    val stats: LocationHistoryStats?,
    val mapFocusType: LocationHistoryMapFocusType,
    val selectedLocationIndex: Int?,
    val days: List<LocationHistoryDayState>,
    val isSatelliteMode: Boolean = false,
    val alertDialog: MainAlertDialog? = null,
) {
    val isDataMissing: Boolean = groupId.isBlank() || memberUserId.isBlank()

    val startDate: LocalDate = selectedDate.minus(DatePeriod(days = dateRangeDays - 1))
    val endDate: LocalDate = selectedDate
    val displayedDays = days.reversed()

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

    val selectedPoint: LocationHistoryPointState? =
        if (mapFocusType is LocationHistoryMapFocusType.LocationPoint) {
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

    val deleteAction = if (isCurrentUser) {
        MemberActionItemState(type = MemberActionType.DELETE_LOCATION)
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
}

data class LocationHistoryStats(
    val formattedTimeRange: String?,
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
) {
    companion object {
        fun calculateStats(
            state: LocationHistoryScreenState,
            valueFormatter: IValueFormatter,
            geofenceManager: IGeofenceManager
        ): LocationHistoryStats? {
            val locations = state.selectedLocations
            if (state.locations.isEmpty()) {
                return null
            }

            // Get location points with valid coordinates required for some data
            val locationPoints = locations.mapNotNull { it.point.loc }.sortedBy { it.tB }

            val formattedTimeRange = valueFormatter.formatTimeRangeForLocationHistory(
                startTimeUTC = locations.first().point.loc?.tA?.isoToLocalDateTime(),
                endTimeUTC = locations.last().point.status.tB.isoToLocalDateTime(),
                rangeStart = state.startDate,
                rangeEnd = state.endDate
            )

            var totalDistanceMeters = 0f
            for (i in 1 until locationPoints.size) {
                val prev = locationPoints[i - 1]
                val curr = locationPoints[i]
                totalDistanceMeters += geofenceManager.distanceBetweenPoints(
                    lat1 = prev.lat,
                    lng1 = prev.lng,
                    lat2 = curr.lat,
                    lng2 = curr.lng
                )
            }

            val batteryLevels = locations.mapNotNull { it.point.status.bat }
            val batteryRange = if (batteryLevels.isNotEmpty()) {
                Pair(batteryLevels.minOrNull() ?: 0, batteryLevels.maxOrNull() ?: 0)
            } else {
                Pair(0, 0)
            }
            val averageBattery = if (batteryLevels.isNotEmpty()) {
                batteryLevels.average().toInt()
            } else {
                0
            }

            val maxSpeedMps = locationPoints.mapNotNull { it.spd }.maxOrNull() ?: 0f

            val elevations = locationPoints.mapNotNull { it.alt }
            val elevationRangeMeters = if (elevations.isNotEmpty()) {
                Pair(elevations.minOrNull() ?: 0f, elevations.maxOrNull() ?: 0f)
            } else {
                Pair(0f, 0f)
            }

            val elevationGainMeters = if (elevations.size >= 2) {
                elevations.last() - elevations.first()
            } else {
                0f
            }

            val networks =
                locations
                    .mapNotNull { it.point.status.net?.let { NetworkType.fromKey(it)?.networkName } }
                    .distinct()

            // Format values
            val formattedDistance = valueFormatter.getDistanceSmall(totalDistanceMeters.toDouble())
            val formattedMaxSpeed = valueFormatter.getSpeedSmall(maxSpeedMps)
            val formattedElevationRange =
                "${valueFormatter.getDistanceSmall(elevationRangeMeters.first.toDouble())} - ${
                    valueFormatter.getDistanceSmall(elevationRangeMeters.second.toDouble())
                }"

            val formattedElevationGain =
                valueFormatter.getDistanceSmall(elevationGainMeters.toDouble())

            return LocationHistoryStats(
                formattedTimeRange = formattedTimeRange,
                totalDistanceMeters = totalDistanceMeters,
                batteryRange = batteryRange,
                averageBattery = averageBattery,
                maxSpeedMps = maxSpeedMps,
                elevationRangeMeters = elevationRangeMeters,
                elevationGainMeters = elevationGainMeters,
                locationEntriesCount = locations.size,
                networks = networks,
                formattedDistance = formattedDistance,
                formattedMaxSpeed = formattedMaxSpeed,
                formattedElevationRange = formattedElevationRange,
                formattedElevationGain = formattedElevationGain
            )
        }
    }
}

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
    data object LocationPoint : LocationHistoryMapFocusType

    val isLocationPoint: Boolean get() = this is LocationPoint
}

data class LocationHistoryDayState(
    val isSelected: Boolean,
    val isWeekend: Boolean,
    val date: LocalDate,
    val formattedDay: String,
    val formattedMonth: String,
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
    data class SelectLocation(val index: Int) : LocationHistoryUiEvent
    data class SelectDevice(val device: UserActiveLocationDeviceState) : LocationHistoryUiEvent
    data class DialogAction(val action: AlertDialogAction) : LocationHistoryUiEvent
    data class OnMarkerClick(val pin: MapPinData) : LocationHistoryUiEvent
    data class OnMapLongPress(val lat: Double, val lng: Double) : LocationHistoryUiEvent
    data class SetHistoryProgress(val progress: Int) : LocationHistoryUiEvent
    data class ChangeMapFocusType(val type: LocationHistoryMapFocusType) : LocationHistoryUiEvent
    data class onDirectionsRequest(val action: MemberNavigateItemState): LocationHistoryUiEvent
    data object onDeleteSelectedLocationRequest: LocationHistoryUiEvent
    data object OnRefocusClick : LocationHistoryUiEvent
    data object OnMapModeSwitch : LocationHistoryUiEvent
    data object OnCloseFocusedItemClick : LocationHistoryUiEvent
    data object OnMapTouch : LocationHistoryUiEvent
    data object OnPreviousMonth : LocationHistoryUiEvent
    data class OnSelectDay(val day: LocalDate) : LocationHistoryUiEvent
    data object ToggleDayRange : LocationHistoryUiEvent
    data object OnCloseScreen : LocationHistoryUiEvent
}

sealed interface LocationHistoryScreenEvent {
    data class OpenNavigation(val event: MapHomeScreenEvent.OpenExternalNavigation) : LocationHistoryScreenEvent
    data object NavigateBack : LocationHistoryScreenEvent
}