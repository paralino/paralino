package app.paralino.android.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.BottomSheetsScreen
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.groups.init.GroupInitScreen
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.safeCast
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.map.asFocusedMarker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun MapHomeScreen(
    appState: MainAppState,
    groupId: String?,
    vm: MapHomeViewModel = getViewModel(),
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
                GroupInitScreen(appState = appState)
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
    val mapViewState = rememberMapViewHomeState(darkMode = isSystemInDarkTheme())

    val uiState by vm.uiState.collectAsState(MapHomeScreenState.Empty())
    rememberSaveable(groupId, isSystemInDarkTheme()) {
        vm.switchGroup(groupId = groupId ?: "")
        ""
    }

    var trackGroup by rememberSaveable(isSystemInDarkTheme()) { mutableStateOf(true) }

    when (val finalState = uiState) {
        is MapHomeScreenState.Ready -> {
            LaunchedEffect(finalState.groupId) {
                logg.tag("location_performance").v("Map active for ${finalState.groupId}")
                vm.locationRefreshSubscription.collect()
            }
        }
        is MapHomeScreenState.Empty -> {}
    }

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
            onTrackGroupClick = {
                trackGroup = !trackGroup
                vm.trackMarker(null)
            },
            onSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.routeSignature)
            },
            onClearMyLocationClick = {
                vm.clearMyLocation(it)
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
            }
        )
    }
}
