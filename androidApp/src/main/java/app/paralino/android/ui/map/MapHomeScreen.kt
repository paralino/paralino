package app.paralino.android.ui.map

import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.BottomSheetsScreen
import app.paralino.android.ui.groups.init.GroupInitScreen
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.safeCast
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.map.*
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
    when(val state = groupState) {
        Resource.NotInitialized -> {}
        is Resource.Ready -> {
            if(state.data.isNotEmpty()) {
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

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
    rememberSaveable(groupId, isSystemInDarkTheme()) {
        vm.switchGroup(groupId = groupId ?: "")
        ""
    }

    var trackGroup by rememberSaveable(isSystemInDarkTheme()) { mutableStateOf(true) }

    if (uiState is Payload.Success) {
        LaunchedEffect(uiState.data?.groupId) {
            logg.tag("location_performance").v("Map active for ${uiState.data?.groupId}")
            vm.locationRefreshSubscription.collect()
        }
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
                            val focusedMarker = uiState.data?.focusedMarker
                            logg.tag("marker_click").v("Place marker clicked: $focusedMarker")
                            vm.trackMarker(it.asFocusedMarker())
                            appState.navController.navigate(
                                AppScreen.MainHome.PlaceDetail.route(
                                    uiState.data?.groupId!!,
                                    it.markerId
                                )
                            )
                        }
                    }
                    trackGroup = false
                    true
                }

                mapViewState.mapAndroidView
            },
            update = { }
        )

        when (val state = uiState) {
            is Payload.Error -> {}
            Payload.Loading -> {}
            is Payload.Success -> {

                val data by rememberUpdatedState(state.data)
                val markers by rememberUpdatedState(data.markers)

                LaunchedEffect(markers) {
                    mapViewState.updateMarkers(list = markers)
                }

                LaunchedEffect(data, trackGroup) {
                    mapViewState.reDrawMarkers(data.focusedMarker, trackGroup)
                }

                @Suppress("USELESS_IS_CHECK")
                AnimatedVisibility(
                    visible = state is Payload.Success,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    MapHomeScreenElements(
                        modifier = Modifier.fillMaxSize(),
                        state = data,
                        isTrackingGroup = trackGroup,
                        focusedMarker = data.focusedMarker,
                        onGroupPickerClick = {
                            appState.uiScope.launch {
                                val groups = vm.getAllGroups()
                                appState.showBottomSheetForResult(
                                    BottomSheetsScreen.GroupPicker(groups)
                                ).safeCast<GroupPickerAction>()?.let {
                                    when(it){
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
                            appState.navController.navigate(AppScreen.MainHome.GroupDetails.route(state.data.groupId))
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
        }

    }
}
