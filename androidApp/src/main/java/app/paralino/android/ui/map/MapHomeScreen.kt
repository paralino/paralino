package app.paralino.android.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.groups.GroupInitScreen
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.groups.GroupPickerAction
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun MapHomeScreen(
    appState: MainAppState,
    groupId: String?,
    vm: MapHomeViewModel = getViewModel(),
    onClose: () -> Unit,
) {
    val groupState by vm.allGroups.collectAsState()
    when (val state = groupState) {
        Resource.NotInitialized -> {}
        is Resource.Ready -> {
            if (state.data.isNotEmpty()) {
                MapHomeContent(
                    appState = appState,
                    startGroupId = groupId,
                    vm = vm
                )
            } else {
                GroupInitScreen(
                    appState = appState,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
fun MapHomeContent(
    appState: MainAppState,
    startGroupId: String?,
    vm: MapHomeViewModel,
) {

    val uiState by vm.uiState.collectAsState(MapHomeScreenState.Empty())
    val isDarkMode = isSystemInDarkTheme()
    rememberSaveable(startGroupId, isDarkMode, uiState.isSatelliteMode) {
        vm.switchGroup(groupId = startGroupId ?: "")
        ""
    }

    Box(modifier = Modifier.fillMaxSize()) {

        MainHomeMapComposable(
            isDarkMode = isDarkMode,
            satelliteMode = uiState.isSatelliteMode,
            state = (uiState as? MapHomeScreenState.Ready),
            onMapLongPress = { lat, lng ->
                val groupId = (uiState as? MapHomeScreenState.Ready)?.groupId
                if (groupId != null) {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceCreate.route(
                            groupId = groupId,
                            placeId = null,
                            lat = lat,
                            lng = lng
                        )
                    )
                }
            },
            onMapTouch = {
                vm.hideFocusedItems()
            },
            onMarkerClick = {
                when (it.pinType) {
                    MapPinType.MEMBER -> {
                        logg.tag("marker_click").v("Member marker clicked")
                        vm.trackMarker(it.asFocusedMarker())
                    }

                    MapPinType.PLACE -> {
                        when (val state = uiState) {
                            is MapHomeScreenState.Ready -> {
                                val focusedMarker = state.focusedMarker
                                logg.tag("marker_click")
                                    .v("Place marker clicked: $focusedMarker")
                                if (focusedMarker?.markerId == it.markerId) {
                                    appState.navController.navigate(
                                        AppScreen.MainHome.PlaceDetail.route(
                                            state.groupId,
                                            it.markerId
                                        )
                                    )
                                } else {
                                    vm.trackMarker(it.asFocusedMarker())
                                }
                            }

                            is MapHomeScreenState.Empty -> {}
                        }
                    }
                }
            }
        )

        val data by rememberUpdatedState(uiState)

        MapHomeScreenElements(
            modifier = Modifier.fillMaxSize(),
            state = data,
            onGroupPickerClick = {
                appState.uiScope.launch {
                    val groups = vm.getAllGroups()
                    appState.showBottomSheetForResult(
                        BottomSheetsScreen.GroupPicker(groups)
                    ).safeCast<GroupPickerAction>()?.let  {
                        when (it) {
                            is GroupPickerAction.GroupPicked -> {
                                vm.switchGroup(it.group.id)
                            }

                            is GroupPickerAction.GroupEditPicked -> {
                                appState.navController.navigate(
                                    AppScreen.MainHome.GroupDetails.route(it.group.id)
                                )
                            }

                            GroupPickerAction.NewGroup -> {
                                appState.navController.navigate(AppScreen.MainHome.GroupCreate.routeSignature)
                            }

                            GroupPickerAction.JoinGroup -> {
                                appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
                            }
                        }
                    }
                }
            },
            onGroupDetailsClick = {
                when (val state = uiState) {
                    is MapHomeScreenState.Ready -> {
                        appState.navController.navigate(AppScreen.MainHome.GroupDetails.route(state.groupId))
                    }

                    is MapHomeScreenState.Empty -> {}
                }
            },
            onSharingStartClick = {
                when (val state = uiState) {
                    is MapHomeScreenState.Ready -> {
                        appState.navController.navigate(AppScreen.MainHome.SharingStart.route(state.groupId))
                    }

                    is MapHomeScreenState.Empty -> {}
                }
            },
            onSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.routeSignature)
            },
            onMemberActionClick = { action, state ->
                vm.memberAction(action, state)
            },
            onMemberClick = {
                when (it) {
                    is GroupMemberItemStateWithLocations -> {}
                    is GroupMemberItemStateWithOneLocation -> {
                        vm.trackMarker(it.asFocusedMarker())
                        // TODO refocus right away
                    }
                }
            },
            onPlaceClick = { state ->
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceDetail.route(
                        groupId = state.groupId,
                        placeId = state.id
                    )
                )
            },
            onMapModeSwitch = {
                vm.switchMapMode()
            },
            onFocusOnPlacesClick = {
                vm.toggleFocusOnPlaces()
            },
            onFocusOnMembersClick = {
                vm.toggleFocusOnMembers()
            }
        )
    }
}
