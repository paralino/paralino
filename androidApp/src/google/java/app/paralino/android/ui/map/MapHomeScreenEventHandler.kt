package app.paralino.android.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.map.MapHomeScreenEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.SwipeableSheetState
import app.paralino.core.ui.map.isFullyExpanded
import app.paralino.core.utils.extensions.openNavigation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun MapHomeScreenEventHandler(
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
                            deviceId = event.deviceId
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
