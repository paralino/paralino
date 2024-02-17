package app.paralino.android.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.groups.GroupInitScreen
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.Resource
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapSheetSelectedContentType
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.groups.GroupPickerAction
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.map.isFullyExpanded
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
    MessageDispatcherEffect(vm, appState)

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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MapHomeContent(
    appState: MainAppState,
    startGroupId: String?,
    vm: MapHomeViewModel,
) {

    val uiState by vm.uiState.collectAsState(MapHomeScreenState.Empty())
    val readyState by rememberUpdatedState(newValue = uiState.asReady())
    val density = LocalDensity.current

    val sheetState = rememberSwipeableState(
        initialValue = BottomSheetPosition.HALF_EXPANDED,
        confirmStateChange = {
            it != BottomSheetPosition.HIDDEN
        }
    )
    val bottomOffset = density.run {
        try {
            sheetState.offset.value.toDp()
        } catch (e: Exception) {
            0.dp
        }
    }

    rememberSaveable(startGroupId) {
        vm.switchGroup(groupId = startGroupId ?: "")
        ""
    }

    BackHandler(
        enabled = readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true || sheetState.isFullyExpanded()
    ) {
        appState.uiScope.launch {
            sheetState.animateTo(BottomSheetPosition.HALF_EXPANDED)
        }
        vm.hideFocusedItem()
    }

    MainMapSheet(
        appState = appState,
        swipeableState = sheetState,
        trackMarker = { vm.trackMarker(it) },
        readyState = readyState,
        onOpenGroupScreen = {
            appState.navController.navigate(
                AppScreen.MainHome.GroupDetails.route(
                    groupId = readyState?.groupId ?: ""
                )
            )
        },
        onOpenPlacesScreen = {
            appState.navController.navigate(
                AppScreen.MainHome.PlacesList.route(
                    groupId = readyState?.groupId ?: ""
                )
            )
        },
        onOpenCreatePlaceScreen = {
            appState.navController.navigate(
                AppScreen.MainHome.PlaceCreate.route(
                    groupId = readyState?.groupId ?: "",
                    placeId = null
                )
            )
        },
        screenContent = {
            MapContent(
                appState = appState,
                uiState = uiState,
                readyState = readyState,
                vm = vm,
                bottomOffset = bottomOffset
            )
        },
        onFocusOnMembersClick = {
            vm.setSelectedContentType(MapSheetSelectedContentType.MEMBERS)
        },
        onFocusOnPlacesClick = {
            vm.setSelectedContentType(MapSheetSelectedContentType.PLACES)
        }
    )
}

@Composable
private fun MapContent(
    appState: MainAppState,
    uiState: MapHomeScreenState,
    readyState: MapHomeScreenState.Ready?,
    vm: MapHomeViewModel,
    bottomOffset: Dp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        MainHomeMapComposable(
            appState = appState,
            uiState = uiState,
            vm = vm,
            mapContentPadding = PaddingValues(
                top = 68.dp,
                bottom = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true) {
                    0.dp
                } else {
                    (this.maxHeight - bottomOffset).coerceIn(0.dp, this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent)
                }
            )
        )

        MapHomeScreenElements(
            state = uiState,
            sheetPeekHeight = (this.maxHeight - bottomOffset).coerceIn(0.dp, this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent),
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
                        appState.navController.navigate(
                            AppScreen.MainHome.GroupDetails.route(
                                state.groupId
                            )
                        )
                    }

                    is MapHomeScreenState.Empty -> {}
                }
            },
            onSharingStartClick = {
                when (val state = uiState) {
                    is MapHomeScreenState.Ready -> {
                        appState.navController.navigate(
                            AppScreen.MainHome.SharingStart.route(
                                state.groupId
                            )
                        )
                    }

                    is MapHomeScreenState.Empty -> {}
                }
            },
            onSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.routeSignature)
            },
            onPlaceClick = { state ->
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceDetail.route(
                        groupId = state.groupId,
                        placeId = state.id
                    )
                )
            },
            onMemberActionClick = { action, state ->
                vm.memberAction(action, state)
            },
            onMemberClick = {
                // TODO refocus right away
                vm.trackMarker(it.asFocusedMarker())
            },
            onToggleLocation = {
                vm.toggleMarkerVisibility(it)
            },
            onMapModeSwitch = {
                vm.switchMapMode()
            },
            onCloseFocusedItem = {
                vm.hideFocusedItem()
            }
        )
    }
}

