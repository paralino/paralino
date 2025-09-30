package app.paralino.android.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.groups.GroupBottomSheetPicker
import app.paralino.android.ui.onboarding.AccountOnboardingScreen
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.models.Resource
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.onboarding.AccountOnboardingViewModel
import app.paralino.core.presentation.map.MapHomeScreenEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapScreenViewSourceType
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.map.SwipeableSheetState
import app.paralino.core.ui.map.isFullyExpanded
import app.paralino.core.ui.map.rememberSwipeableSheetState
import app.paralino.core.utils.extensions.openNavigation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun MapHomeScreen(
    appState: MainAppState,
    groupId: String?,
    vm: AccountOnboardingViewModel = koinViewModel(),
    onClose: () -> Unit,
) {
    val groupState by vm.uiState.collectAsState(Resource.NotInitialized)
    when (val state = groupState) {
        Resource.NotInitialized -> {}
        is Resource.Ready -> {
            if (state.data.canDisplayMap) {
                MapHomeContent(
                    appState = appState,
                    startGroupId = groupId,
                )
            } else {
                AccountOnboardingScreen(
                    appState = appState,
                    vm = vm
                )
            }
        }
    }
}

@Composable
private fun MapHomeContent(
    appState: MainAppState,
    startGroupId: String?,
    vm: MapHomeViewModel = koinViewModel(),
) {
    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState()
    val readyState by rememberUpdatedState(newValue = uiState.asReady())
    val density = LocalDensity.current
    val pickerState = rememberGroupBottomSheetPickerState()

    val sheetState = rememberSwipeableSheetState(
        initialValue = BottomSheetPosition.HIDDEN,
        collapsedAnchorBottomPaddingPixels = 0f,
        confirmValueChange = {
            it != BottomSheetPosition.HIDDEN
        }
    )

    MapScreenEventHandler(
        vm = vm,
        appState = appState,
        readyState = readyState,
        sheetState = sheetState,
        showGroupPicker = {
            appState.uiScope.launch {
                pickerState.show(it)
            }
        }
    )

    rememberSaveable(startGroupId) {
        vm.onEvent(
            MapHomeScreenUiEvent.SwitchViewSource(
                viewSource = MapScreenViewSourceType.DefaultGroupOrSpace(startGroupId?.asGroupOrSpaceId())
            )
        )
        // have to keep empty state so it keeps it saved and only executed this once
        ""
    }

    val dialog = uiState.alertDialog
    dialog?.Show {
        vm.onEvent(MapHomeScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    val bottomOffset by remember {
        derivedStateOf {
            density.run {
                try {
                    sheetState.offset.toDp()
                } catch (e: Exception) {
                    0.dp
                }
            }
        }
    }

    MainMapSheet(
        appState = appState,
        swipeableState = sheetState,
        screenPaddingValues = WindowInsets.safeDrawing.asPaddingValues(),
        mapUiState = uiState,
        onEvent = { vm.onEvent(it) },
        screenContent = {
            MapContent(
                appState = appState,
                uiState = uiState,
                readyState = readyState,
                bottomOffset = bottomOffset,
                onEvent = { vm.onEvent(it) }
            )
        }
    )

    GroupBottomSheetPicker(
        state = pickerState,
        onClick = {
            appState.uiScope.launch {
                pickerState.hide()
                when (it) {
                    is GroupPickerAction.GroupPicked -> {
                        vm.onEvent(
                            MapHomeScreenUiEvent.SwitchViewSource(
                                MapScreenViewSourceType.DefaultGroupOrSpace(it.group.id)
                            )
                        )
                    }

                    is GroupPickerAction.GroupEditPicked -> {
                        vm.onEvent(MapHomeScreenUiEvent.OpenGroupDetails(it.group.id))
                    }

                    GroupPickerAction.NewGroup -> {
                        vm.onEvent(MapHomeScreenUiEvent.TryCreateGroup)
                    }

                    GroupPickerAction.JoinGroup -> {
                        appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
                    }

                    GroupPickerAction.OpenSubscriptions -> {
                        appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
                    }

                    GroupPickerAction.Close -> {}
                }
            }
        }
    )
}

@Composable
private fun MapContent(
    appState: MainAppState,
    uiState: MapHomeScreenState,
    readyState: MapHomeScreenState.Ready?,
    bottomOffset: Dp,
    onEvent: (MapHomeScreenUiEvent) -> Unit
) {
    var cardHeight by remember { mutableStateOf(0.dp) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        MainHomeMapComposable(
            appState = appState,
            uiState = uiState,
            onEvent = onEvent,
            mapContentPadding = PaddingValues(
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 64.dp,
                bottom = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true) {
                    (cardHeight + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding()).coerceIn(
                        minimumValue = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding(),
                        maximumValue = this.maxHeight
                    )
                } else {
                    (this.maxHeight - bottomOffset).coerceIn(
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
                    )
                }
            )
        )

        MapHomeScreenElements(
            modifier = Modifier,
            state = uiState,
            topPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 4.dp,
            sheetPeekHeight = (this.maxHeight - bottomOffset).coerceIn(
                minimumValue = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding(),
                maximumValue = this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
            ),
            cardHeight = { cardHeight = it },
            onEvent = onEvent
        )
    }
}

@Composable
fun MapScreenEventHandler(
    vm: MapHomeViewModel,
    appState: MainAppState,
    readyState: MapHomeScreenState.Ready?,
    sheetState: SwipeableSheetState,
    showGroupPicker: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
) {
    val context = LocalContext.current

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        messageDispatcher = vm,
        notificationStatusProvider = vm.deviceApiProvider.notificationStatusProvider
    )

    BackHandler(
        enabled = readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true ||
                (sheetState.isFullyExpanded() && readyState != null)
    ) {
        appState.uiScope.launch {
            sheetState.animateTo(BottomSheetPosition.HALF_EXPANDED)
        }
        vm.onEvent(MapHomeScreenUiEvent.OnCloseFocusedItemClick)
    }

    LifecycleStartEffect(Unit) {
        vm.onEvent(MapHomeScreenUiEvent.UiLifecycleChange(active = true))
        onStopOrDispose {
            vm.onEvent(MapHomeScreenUiEvent.UiLifecycleChange(active = false))
        }
    }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                MapHomeScreenEvent.OpenGroupCreateScreen -> {
                    appState.navController.navigate(AppScreen.MainHome.GroupCreate.route())
                }

                MapHomeScreenEvent.OpenPaywall -> {
                    appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
                }

                is MapHomeScreenEvent.OpenCreatePlace -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceCreate.route(
                            groupId = vm.uiState.value.asReady()?.groupOrSpaceId?.id ?: "",
                            placeId = null
                        )
                    )
                }

                is MapHomeScreenEvent.OpenGroupDetails -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.GroupDetails.route(
                            groupId = event.groupId
                        )
                    )
                }

                is MapHomeScreenEvent.OpenGroupPicker -> {
                    val groupPickerState = vm.getGroupPickerState(showPersonalSpace = event.showPersonalSpace)
                    showGroupPicker(groupPickerState)
                }

                is MapHomeScreenEvent.OpenPlaceDetails -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceDetail.route(
                            groupId = event.groupId,
                            placeId = event.placeId
                        )
                    )
                }

                is MapHomeScreenEvent.OpenPlaces -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlacesList.route(
                            groupId = event.groupId
                        )
                    )
                }

                MapHomeScreenEvent.OpenSettings -> {
                    appState.navController.navigate(AppScreen.Settings.routeSignature)
                }

                is MapHomeScreenEvent.OpenSharingStart -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.SharingStart.route(event.groupId)
                    )
                }

                is MapHomeScreenEvent.OpenExternalNavigation -> {
                    context.openNavigation(event)
                }

                is MapHomeScreenEvent.OpenLocationHistory -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.LocationHistory.route(
                            groupId = event.groupId,
                            memberUserId = event.memberUserId,
                        )
                    )
                }

                is MapHomeScreenEvent.ShowPlanLimitAlert -> {
                    // This would need to be handled by showing an alert dialog
                    // For now, we'll just navigate to paywall
                    appState.navController.navigate(AppScreen.MainHome.BillingPaywall.route())
                }

                MapHomeScreenEvent.OpenPersonalSpace -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PersonalSpace.route()
                    )
                }

                is MapHomeScreenEvent.SharePersonalCode -> {
                    context.share(event.content)
                }

                MapHomeScreenEvent.AskNotificationPermission -> {
                    notificationPermissionHandler.askNotificationPermission()
                }

                MapHomeScreenEvent.OpenPersonalCodes -> {
                    appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
                }

                null -> {}
            }
        }
    }
}