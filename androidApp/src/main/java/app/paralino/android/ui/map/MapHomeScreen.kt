package app.paralino.android.ui.map

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paralino.android.ui.groups.GroupBottomSheetPicker
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.android.ui.groups.rememberGroupBottomSheetPickerState
import app.paralino.android.ui.history.LocationHistoryScreenEventHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.onboarding.AccountOnboardingScreen
import app.paralino.core.models.Resource
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapScreenViewSourceType
import app.paralino.core.presentation.map.asReady
import app.paralino.core.presentation.onboarding.AccountOnboardingViewModel
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.rememberSwipeableSheetState
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

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val historyUiState = vm.historyUiState.collectAsStateWithLifecycle().value
    val readyState by rememberUpdatedState(newValue = uiState.asReady())
    val density = LocalDensity.current
    val pickerState = rememberGroupBottomSheetPickerState()

    val sheetState = rememberSwipeableSheetState(
        initialValue = BottomSheetPosition.HIDDEN,
        collapsedAnchorBottomPaddingPixels = 0f,
        bottomScreenCornerRadius = vm.deviceApiProvider.deviceCornerRadius(),
        confirmValueChange = {
            it != BottomSheetPosition.HIDDEN
        }
    )

    MapHomeScreenEventHandler(
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

    if (historyUiState != null) {
        LocationHistoryScreenEventHandler(
            appState = appState,
            uiState = historyUiState,
            events = vm.historyEvents,
            onDialogEvent = {
                vm.onEvent(it)
            },
            onCloseHistory = {
                vm.onEvent(MapHomeScreenUiEvent.CloseLocationHistory)
            }
        )
    }

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
        onEvent = vm::onEvent,
        screenContent = {
            HomeScreenMapWithElements(
                appState = appState,
                uiState = uiState,
                sheetState = sheetState,
                readyState = readyState,
                historyUiState = historyUiState,
                bottomOffset = bottomOffset,
                onMainEvent = vm::onEvent,
                onHistoryEvent = vm::onEvent
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
