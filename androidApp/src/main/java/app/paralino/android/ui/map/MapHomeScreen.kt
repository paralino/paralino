package app.paralino.android.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
import com.mapbox.maps.Style
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
                    groupId = groupId,
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
    groupId: String?,
    vm: MapHomeViewModel,
) {

    val uiState by vm.uiState.collectAsState(MapHomeScreenState.Empty())
    val isDarkMode = isSystemInDarkTheme()
    val mapViewState = rememberMapViewHomeState(
        darkMode = isDarkMode,
        satellite = uiState.isSatelliteMode
    )
    rememberSaveable(groupId, isDarkMode, uiState.isSatelliteMode) {
        vm.switchGroup(groupId = groupId ?: "")
        mapViewState.mapAndroidView.getMapboxMap().loadStyleUri(
            if (uiState.isSatelliteMode) {
                Style.SATELLITE_STREETS
            } else if (isDarkMode) {
                Style.DARK
            } else {
                Style.MAPBOX_STREETS
            }
        )
        ""
    }

    var trackGroup by rememberSaveable(isSystemInDarkTheme()) { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {

        // map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapViewState.initMap()
                mapViewState.onMapTouch = {
                    trackGroup = false
                    vm.trackMarker(null)
                    false
                }
                mapViewState.onMarkerClick = {
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
                                    vm.trackMarker(it.asFocusedMarker())
                                    appState.navController.navigate(
                                        AppScreen.MainHome.PlaceDetail.route(
                                            state.groupId,
                                            it.markerId
                                        )
                                    )
                                }

                                is MapHomeScreenState.Empty -> {}
                            }
                        }
                    }
                    trackGroup = false
                    true
                }

                mapViewState.mapAndroidView
            },
            update = { }
        )

        val data by rememberUpdatedState(uiState)

        when (val state = data) {
            is MapHomeScreenState.Ready -> {
                val markers by rememberUpdatedState(state.markers)
                LaunchedEffect(markers) {
                    mapViewState.updateMarkers(list = markers)
                }

                LaunchedEffect(data, trackGroup) {
                    mapViewState.reDrawMarkers(state.focusedMarker, trackGroup)
                }
            }

            is MapHomeScreenState.Empty -> {}
        }

        MapHomeScreenElements(
            modifier = Modifier.fillMaxSize(),
            state = data,
            isTrackingGroup = trackGroup,
            focusedMarker = (data as? MapHomeScreenState.Ready)?.focusedMarker,
            onGroupPickerClick = {
                appState.uiScope.launch {
                    val groups = vm.getAllGroups()
                    appState.showBottomSheetForResult(
                        BottomSheetsScreen.GroupPicker(groups)
                    ).safeCast<GroupPickerAction>()?.let {
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
            onTrackGroupClick = {
                trackGroup = !trackGroup
                vm.trackMarker(null)
            },
            onSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.routeSignature)
            },
            onMemberActionClick = { action, state ->
                vm.memberAction(action, state)
            },
            onMemberClick = {
                trackGroup = false
                when (it) {
                    is GroupMemberItemStateWithLocations -> {}
                    is GroupMemberItemStateWithOneLocation -> {
                        vm.trackMarker(it.asFocusedMarker())
                        mapViewState.focusToMarker(it.keyId)
                    }
                }
            },
            onMapModeSwitch = {
                vm.switchMapMode()
            }
        )
    }
}
