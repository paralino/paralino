package app.paralino.android.ui.history

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.presentation.history.LocationHistoryScreenEvent
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.history.LocationHistoryViewModel
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.isFullyExpanded
import app.paralino.core.ui.map.rememberSwipeableSheetState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LocationHistoryScreen(
    appState: MainAppState,
    groupId: String,
    memberUserId: String,
    vm: LocationHistoryViewModel = koinViewModel(),
) {
    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState()
    val density = LocalDensity.current
    val context = LocalContext.current

    val sheetState = rememberSwipeableSheetState(
        initialValue = BottomSheetPosition.default,
        collapsedAnchorBottomPaddingPixels = WindowInsets.navigationBars.getBottom(density)
            .toFloat(),
        confirmValueChange = {
            it != BottomSheetPosition.HIDDEN
        }
    )

    val bottomOffset = density.run {
        try {
            sheetState.offset.toDp()
        } catch (e: Exception) {
            0.dp
        }
    }

    LifecycleStartEffect(Unit) {
        vm.onEvent(LocationHistoryUiEvent.UiLifecycleChange(active = true))
        onStopOrDispose {
            vm.onEvent(LocationHistoryUiEvent.UiLifecycleChange(active = false))
        }
    }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is LocationHistoryScreenEvent.Error -> TODO()
                LocationHistoryScreenEvent.NavigateBack -> {
                    appState.navController.popBackStack()
                }
                null -> TODO()
            }
        }
    }

    rememberSaveable<String>(groupId, memberUserId) {
        vm.loadHistory(groupId = groupId, memberUserId = memberUserId)
        ""
    }

    BackHandler(
        enabled = uiState.mapFocusType.isLocationPoint || sheetState.isFullyExpanded()
    ) {
        appState.uiScope.launch {
            sheetState.animateTo(BottomSheetPosition.HALF_EXPANDED)
        }
        vm.onEvent(LocationHistoryUiEvent.OnCloseFocusedItemClick)
    }

    val dialog = uiState.alertDialog
    dialog?.Show {
        vm.onEvent(LocationHistoryUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    MapContent(
        uiState = uiState,
        bottomOffset = bottomOffset,
        onEvent = { vm.onEvent(it) }
    )
}

@Composable
private fun MapContent(
    uiState: LocationHistoryScreenState,
    bottomOffset: Dp,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    var cardHeight by remember { mutableStateOf(0.dp) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LocationHistoryMapContent(
            uiState = uiState,
            onEvent = onEvent,
            mapContentPadding = PaddingValues(
                top = 68.dp,
                bottom = (cardHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).coerceIn(
                    minimumValue = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    maximumValue = this.maxHeight
                )
            )
        )

        LocationHistoryScreenElements(
            modifier = Modifier,
            uiState = uiState,
            topPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
            cardHeight = { cardHeight = it },
            onEvent = onEvent
        )
    }
}
