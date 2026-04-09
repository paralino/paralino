package app.paralino.core.presentation.history

import androidx.compose.ui.unit.Dp
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.location.LocationHistoryPointId
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberNavigateItemState
import app.paralino.core.presentation.map.MapHomeScreenEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.generic.MapInteractionsType
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
    val groupOrSpaceId: GroupOrSpaceId,
    val memberUserId: String,
    val preselectedDeviceId: String?,
    val groupName: String?,
    val groupImage: ImageModel?,
    val isGroupOwner: Boolean,
    val userName: String?,
    val userImage: ImageModel?,
    val isCurrentUser: Boolean,
    val isPersonalHistory: Boolean,
    val selectedDate: LocalDate,
    val isDeletingEntry: Boolean,
    val deletionRangeStartIndex: Int?,
    val isLoadingData: Boolean,
    val isLoadingLocations: Boolean,
    val showPolyline: Boolean,
    val showPins: Boolean,
    val clusteringPinsMode: LocationHistoryClusteringPinsViewMode,
    val clusteringPointsMode: LocationHistoryClusteringPointsViewMode,
    val isFilterOpened: Boolean,
    val mapInteractionType: MapInteractionsType,
    private val devices: List<UserActiveLocationDeviceState>,
    val locations: List<LocationHistoryPointState>,
    val stats: LocationHistoryStats?,
    val dataViewType: LocationHistoryDataViewType,
    val focusType: LocationHistoryScreenFocusType,
    val previousFocusType: LocationHistoryScreenFocusType,
    val selectedLocationId: String?,
    val daySelectorState: LocationHistoryDateSelectorState,
    val planConfig: PlanConfig?,
    val alertDialog: MainAlertDialog? = null,
) {
    val startDate: LocalDate = selectedDate.minus(DatePeriod(days = daySelectorState.dateRangeDays?.minus(1) ?: 0))
        .let {
            if (daySelectorState.earliestDate != null) {
                it.coerceAtLeast(daySelectorState.earliestDate)
            } else {
                it
            }
        }
    val endDate: LocalDate = selectedDate

    val deviceType: DeviceType = DeviceType.current
    private val displayLocations = when (clusteringPointsMode) {
        LocationHistoryClusteringPointsViewMode.DEFAULT_CLUSTERING,
        LocationHistoryClusteringPointsViewMode.CLUSTERING -> {
            locations.flatMap {
                when (it) {
                    is LocationHistoryPointState.Cluster -> {
                        if (it.isExpanded) {
                            it.points
                        } else {
                            listOf(it)
                        }
                    }
                    is LocationHistoryPointState.Point -> listOf(it)
                }
            }
        }

        LocationHistoryClusteringPointsViewMode.DEFAULT_DETAILED,
        LocationHistoryClusteringPointsViewMode.DETAILED -> {
            locations.flatMap {
                when (it) {
                    is LocationHistoryPointState.Cluster -> it.points
                    is LocationHistoryPointState.Point -> listOf(it)
                }
            }
        }
    }

    val selectedLocations: List<LocationHistoryPointState> = displayLocations.filter { entry ->
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

    val activeDevices: List<UserActiveLocationDeviceState> = devices.map { device ->
        when (device) {
            is UserActiveLocationDeviceState.Known -> {
                device.copy(
                    hasLocations = displayLocations.any { it.locationId == device.locationId }
                )
            }

            is UserActiveLocationDeviceState.Unknown -> {
                device.copy(
                    hasLocations = displayLocations.any {
                        device.ids.contains(it.locationId) || it.locationId == null
                    }
                )
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
                    groupId = groupOrSpaceId,
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
                        groupId = groupOrSpaceId,
                        location = it,
                        name = userName,
                        image = userImage,
                        timeInfo = null
                    )
                }
            }
        }
    }

    val hasPreviousLocation: Boolean = locationIndex > 0
    val hasNextLocation: Boolean = locationIndex < selectedLocations.size - 1

    val deleteAction: MemberActionItemState? =
        if (
            isCurrentUser &&
            selectedPoint != null &&
            (
                    // for current device we can delete any location
                    devices.firstOrNull { it.locationId == selectedPoint.locationId }?.isCurrent == true ||
                            // for other devices we can only delete fully synced locations
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

    val rawDataAction: RawPresentationDataType.LocationHistoryPointPresentation? = if (isCurrentUser && selectedPoint != null) {
        RawPresentationDataType.LocationHistoryPointPresentation(
            groupOrSpaceId = groupOrSpaceId.id,
            memberUserId = memberUserId,
            pointIds = when (selectedPoint) {
                is LocationHistoryPointState.Cluster -> {
                    selectedPoint.points.mapNotNull {
                        if (!it.isCachedLocation) {
                            LocationHistoryPointId(
                                entryId = it.entryId,
                                pointId = it.pointId
                            )
                        } else {
                            null
                        }
                    }
                }
                is LocationHistoryPointState.Point -> {
                    if (!selectedPoint.isCachedLocation) {
                        listOf(
                            LocationHistoryPointId(
                                entryId = selectedPoint.entryId,
                                pointId = selectedPoint.pointId
                            )
                        )
                    } else {
                        emptyList()
                    }
                }
            }
        ).takeIf { it.pointIds.isNotEmpty() }
    } else {
        null
    }

    val locationsToDelete = if (deletionRangeStartIndex != null && selectedLocationId != null) {
        val rangeStart = min(deletionRangeStartIndex, locationIndex)
        val rangeEnd = max(deletionRangeStartIndex, locationIndex)
        try {
            selectedLocations.subList(
                fromIndex = rangeStart,
                toIndex = rangeEnd + 1
            ).flatMap {
                when (it) {
                    is LocationHistoryPointState.Cluster -> it.points
                    is LocationHistoryPointState.Point -> listOf(it)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    } else {
        emptyList()
    }

    fun canSelectDate(date: LocalDate): Boolean {
        val now = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (date > now) return false
        if (daySelectorState.earliestDate == null) return true
        if (daySelectorState.earliestDate > now) return false
        if (date >= daySelectorState.earliestDate) return true

        return false
    }

    companion object {
        const val maxDateRangeDays: Int = 15
        fun emptyState(): LocationHistoryScreenState {
            val selectedDate = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            return LocationHistoryScreenState(
                groupOrSpaceId = "".asGroupOrSpaceId(),
                memberUserId = "",
                preselectedDeviceId = null,
                groupName = null,
                groupImage = null,
                userName = null,
                userImage = null,
                isCurrentUser = false,
                isPersonalHistory = false,
                isGroupOwner = false,
                selectedDate = selectedDate,
                isDeletingEntry = false,
                deletionRangeStartIndex = null,
                isLoadingLocations = true,
                isLoadingData = true,
                showPolyline = true,
                showPins = true,
                clusteringPinsMode = LocationHistoryClusteringPinsViewMode.DEFAULT_CLUSTERING,
                isFilterOpened = false,
                locations = emptyList(),
                devices = emptyList(),
                stats = null,
                mapInteractionType = MapInteractionsType.MINIMIZE,
                clusteringPointsMode = LocationHistoryClusteringPointsViewMode.DEFAULT_CLUSTERING,
                dataViewType = LocationHistoryDataViewType.Stats,
                focusType = LocationHistoryScreenFocusType.OVERVIEW(isCollapsed = false, isForceCollapsedRequested = false),
                previousFocusType = LocationHistoryScreenFocusType.OVERVIEW(isCollapsed = false, isForceCollapsedRequested = false),
                selectedLocationId = null,
                daySelectorState = LocationHistoryDateSelectorState(maxDateRangeDays = null),
                planConfig = null,
                alertDialog = null
            )
        }
    }
}

data class LocationHistoryScreenUiState(
    // Constructor fields from LocationHistoryScreenState
    val groupOrSpaceId: GroupOrSpaceId,
    val groupName: String?,
    val groupImage: ImageModel?,
    val isGroupOwner: Boolean,
    val userName: String?,
    val userImage: ImageModel?,
    val isCurrentUser: Boolean,
    val isPersonalHistory: Boolean,
    val selectedDate: LocalDate,
    val isDeletingEntry: Boolean,
    val isLoadingData: Boolean,
    val isLoadingLocations: Boolean,
    val showPolyline: Boolean,
    val showPins: Boolean,
    val clusteringPinsMode: LocationHistoryClusteringPinsViewMode,
    val isFilterOpened: Boolean,
    val stats: LocationHistoryStats?,
    val clusteringPointsMode: LocationHistoryClusteringPointsViewMode,
    val dataViewType: LocationHistoryDataViewType,
    val focusType: LocationHistoryScreenFocusType,
    val previousFocusType: LocationHistoryScreenFocusType,
    val selectedLocationId: String?,
    val daySelectorState: LocationHistoryDateSelectorState,
    val alertDialog: MainAlertDialog? = null,
    
    // Computed properties from LocationHistoryScreenState
    val mapInteractionType: MapInteractionsType,
    val deviceType: DeviceType,
    val selectedLocations: List<LocationHistoryPointState>,
    val activeDevices: List<UserActiveLocationDeviceState>,
    val locationIndex: Int,
    val hasPreviousLocation: Boolean,
    val hasNextLocation: Boolean,
    val selectedPoint: LocationHistoryPointState?,
    val selectedMapPinData: MapPinData?,
    val deleteAction: MemberActionItemState?,
    val navigateAction: MemberNavigateItemState?,
    val rawDataAction: RawPresentationDataType.LocationHistoryPointPresentation?,
    val locationsToDeleteSize: Int
) {
    val canCollapseMapHeader = when (mapInteractionType) {
        MapInteractionsType.MINIMIZE -> focusType.isCollapsed
        MapInteractionsType.COLLAPSED -> true
        else -> false
    }

    val canCollapseMapCards = when (mapInteractionType) {
        MapInteractionsType.MINIMIZE,
        MapInteractionsType.COLLAPSED -> true
        else -> focusType.isForceCollapsedRequested
    }

    val isCardCollapsed: Boolean = focusType.isCollapsed && canCollapseMapCards

    companion object {
        fun fromScreenState(screenState: LocationHistoryScreenState): LocationHistoryScreenUiState {
            return LocationHistoryScreenUiState(
                // Constructor fields
                groupOrSpaceId = screenState.groupOrSpaceId,
                groupName = screenState.groupName,
                groupImage = screenState.groupImage,
                isGroupOwner = screenState.isGroupOwner,
                userName = screenState.userName,
                userImage = screenState.userImage,
                isCurrentUser = screenState.isCurrentUser,
                isPersonalHistory = screenState.isPersonalHistory,
                selectedDate = screenState.selectedDate,
                isDeletingEntry = screenState.isDeletingEntry,
                isLoadingData = screenState.isLoadingData,
                isLoadingLocations = screenState.isLoadingLocations,
                showPolyline = screenState.showPolyline,
                showPins = screenState.showPins,
                clusteringPinsMode = screenState.clusteringPinsMode,
                clusteringPointsMode = screenState.clusteringPointsMode,
                isFilterOpened = screenState.isFilterOpened,
                stats = screenState.stats,
                dataViewType = screenState.dataViewType,
                focusType = screenState.focusType,
                previousFocusType = screenState.previousFocusType,
                selectedLocationId = screenState.selectedLocationId,
                daySelectorState = screenState.daySelectorState,
                alertDialog = screenState.alertDialog,
                
                // Computed properties
                mapInteractionType = screenState.mapInteractionType,
                deviceType = screenState.deviceType,
                selectedLocations = screenState.selectedLocations,
                activeDevices = screenState.activeDevices,
                locationIndex = screenState.locationIndex,
                hasNextLocation = screenState.hasNextLocation,
                hasPreviousLocation = screenState.hasPreviousLocation,
                selectedPoint = screenState.selectedPoint,
                selectedMapPinData = screenState.selectedMapPinData,
                deleteAction = screenState.deleteAction,
                navigateAction = screenState.navigateAction,
                rawDataAction = screenState.rawDataAction,
                locationsToDeleteSize = screenState.locationsToDelete.size
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
    abstract val hasLocations: Boolean

    data class Known(
        override val locationId: String,
        override val deviceId: String,
        override val deviceName: String,
        override val isSelected: Boolean,
        override val isCurrent: Boolean,
        override val hasLocations: Boolean
    ) : UserActiveLocationDeviceState()

    data class Unknown(
        val ids: List<String?>,
        override val deviceName: String,
        override val isSelected: Boolean,
        override val hasLocations: Boolean
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
        override val pointId: String,
        override val locationTime: String?,
        val points: List<Point>,
        override val lat: Double,
        override val lng: Double,
        override val accuracy: Float,
        val battery: String?,
        val isExpanded: Boolean
    ): LocationHistoryPointState {
        override val statusTime: String? = null
        override val formattedElevation: String? = null
        override val formattedSpeed: String? = null
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

enum class LocationHistoryClusteringPointsViewMode {
    DEFAULT_CLUSTERING,
    DEFAULT_DETAILED,
    CLUSTERING,
    DETAILED;

    val isClusteringOn: Boolean get() = this == DEFAULT_CLUSTERING || this == CLUSTERING
}

enum class LocationHistoryClusteringPinsViewMode {
    DEFAULT_CLUSTERING,
    DEFAULT_DETAILED,
    CLUSTERING,
    DETAILED;

    val isClusteringOn: Boolean get() = this == DEFAULT_CLUSTERING || this == CLUSTERING
}

sealed interface LocationHistoryScreenFocusType {
    val isCollapsed: Boolean
    val isForceCollapsedRequested: Boolean

    data class NONE_COLLAPSED(
        override val isForceCollapsedRequested: Boolean
    ) : LocationHistoryScreenFocusType {
        override val isCollapsed: Boolean = true
    }

    data class OVERVIEW(
        override val isForceCollapsedRequested: Boolean,
        override val isCollapsed: Boolean
    ) : LocationHistoryScreenFocusType

    data class MEMBER(
        override val isCollapsed: Boolean,
        override val isForceCollapsedRequested: Boolean,
        val isFocused: Boolean,
    ) : LocationHistoryScreenFocusType

    val isMember: Boolean get() = this is MEMBER
    val isMemberFocused: Boolean get() = this is MEMBER && this.isFocused
    val isMemberNotFocused: Boolean get() = this is MEMBER && !this.isFocused
    val isInOverview: Boolean get() = this is OVERVIEW
}

data class LocationHistoryDateSelectorState(
    val maxDateRangeDays: Int?,
    val days: List<LocationHistoryDayState> = emptyList(),
    val daysInLoading: List<LocalDate> = emptyList(),
    val isLimitedHistoryPlanMode: Boolean = false,
    val startAction: LocationHistoryDateSelectionAction = LocationHistoryDateSelectionAction.NONE,
    val dateRangeDays: Int? = null,
    val forceFocusOnSelectedDay: Boolean = false,
    val earliestDate: LocalDate? = null,
) {
    val isMaxRange: Boolean = if (maxDateRangeDays != null && dateRangeDays != null) {
        maxDateRangeDays <= dateRangeDays
    } else {
        false
    }
    val displayedDays = days.reversed()
        .map {
            it.copy(
                isLoading = !isLimitedHistoryPlanMode && daysInLoading.contains(it.date)
            )
        }
}

enum class LocationHistoryDateSelectionAction {
    END, LOAD_MODE, UPGRADE, ASK_FOR_UPGRADE, NONE
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
    data class ReloadHistory(
        val groupId: String,
        val memberUserId: String,
        val preselectedDeviceId: String?
    ) : LocationHistoryUiEvent
    data class SelectDevice(val device: UserActiveLocationDeviceState) : LocationHistoryUiEvent
    data class DialogAction(val action: AlertDialogAction) : LocationHistoryUiEvent
    data class OnMarkerClick(val pin: MapPinData) : LocationHistoryUiEvent
    data class OnPointClicked(val point: LocationHistoryPointState) : LocationHistoryUiEvent
    data class OnPointIdClicked(val pointId: String) : LocationHistoryUiEvent
    data class ExpandCluster(val cluster: LocationHistoryPointState.Cluster) : LocationHistoryUiEvent
    data class OnMapLongPress(val lat: Double, val lng: Double) : LocationHistoryUiEvent
    data class SetHistoryProgress(val progress: Int) : LocationHistoryUiEvent
    data class ChangeMapDataViewType(val type: LocationHistoryDataViewType) : LocationHistoryUiEvent
    data class OnDirectionsRequest(val action: MemberNavigateItemState) : LocationHistoryUiEvent
    data class OnRawDataPointClicked(val action: RawPresentationDataType.LocationHistoryPointPresentation) : LocationHistoryUiEvent
    data class OnScreenDensityChange(val maxWidthDp: Dp, val maxHeightDp: Dp) : LocationHistoryUiEvent
    data object NavigateHistoryNext : LocationHistoryUiEvent
    data object NavigateHistoryPrevious : LocationHistoryUiEvent
    data object ToggleClusterLocations : LocationHistoryUiEvent
    data object ToggleShowPins : LocationHistoryUiEvent
    data object ToggleShowPolyline : LocationHistoryUiEvent
    data object ToggleClusterPins : LocationHistoryUiEvent
    data object OnDeleteSelectedLocationRequest : LocationHistoryUiEvent
    data class OnSelectFocusType(val type: LocationHistoryScreenFocusType) : LocationHistoryUiEvent
    data object OnFocusOnMemberClick: LocationHistoryUiEvent
    data object ToggleExpandedCollapseFocusType : LocationHistoryUiEvent
    data object OnMapTouch : LocationHistoryUiEvent
    data class OnMapClick(val location: LatLng) : LocationHistoryUiEvent
    data object ToggleMapFilter : LocationHistoryUiEvent
    data class OnDateSelectorAction(val action: LocationHistoryDateSelectionAction) : LocationHistoryUiEvent
    data class OnSelectDay(val day: LocalDate) : LocationHistoryUiEvent
    data class OnLongSelectDay(val day: LocalDate) : LocationHistoryUiEvent
    data object ToggleDayRange : LocationHistoryUiEvent
    data object OnCloseScreen : LocationHistoryUiEvent
    data object OpenLocationSettings : LocationHistoryUiEvent
}

sealed interface LocationHistoryScreenEvent {
    data class OpenNavigation(
        val event: MapHomeScreenEvent.OpenExternalNavigation
    ) : LocationHistoryScreenEvent

    data class OpenRawDataScreen(
        val type: RawPresentationDataType
    ) : LocationHistoryScreenEvent

    data object NavigateBack : LocationHistoryScreenEvent
    data object OpenHistorySettings : LocationHistoryScreenEvent
    data object OpenPaywall : LocationHistoryScreenEvent
}