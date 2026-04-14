package app.paralino.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import app.paralino.core.logg
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.history.LocationHistoryScreenEvent
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenEvent
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.asReady
import app.paralino.core.models.settings.RawPresentationDataType
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.history.LocationHistoryMapCardUi
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MainMapSheet
import app.paralino.core.ui.map.MapHomeElementMainCards
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.map.rememberSwipeableSheetState
import app.paralino.core.ui.theme.LocalScreenDimensionType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.UIKit.UIViewController

object MainMapScreen {
    @OptIn(ExperimentalComposeUiApi::class)
    fun MapSheet(
        vm: MapHomeViewModel,
        paddingValues: AppleScreenPaddingValues,
        onOpenPlacesScreen: (String) -> Unit,
        onOpenCreatePlaceScreen: (groupId: String, location: LatLng?) -> Unit,
        onSheetOffsetChange: (Float) -> Unit,
        onGroupPickerClick: (Boolean) -> Unit,
        onGroupDetailsClick: (String) -> Unit,
        onSharingStartClick: (String) -> Unit,
        onHistorySettingsClick: (String) -> Unit,
        onMemberInviteClick: (String) -> Unit,
        onSettingsClick: () -> Unit,
        onPlaceClick: (MapHomeScreenEvent.OpenPlaceDetails) -> Unit,
        onDirectionsRequest: (MapHomeScreenEvent.OpenExternalNavigation) -> Unit,
        onCreateGroup: () -> Unit,
        openPaywall: () -> Unit,
        openPersonalSpace: () -> Unit,
        openPersonalCodes: () -> Unit,
        openRawDataScreen: (RawPresentationDataType) -> Unit,
        onSharePersonalCode: (String) -> Unit,
        askNotificationPermission: () -> Unit,
        mapUIView: () -> UIViewController,
    ) = ComposeUIViewController() {
        RootScreenIOS {
            MessageDispatcherEffect(vm, it)

            val uiState by vm.uiState.collectAsState()
            val historyUiState = vm.historyUiState.collectAsState().value
            val readyState = rememberUpdatedState(uiState.asReady()).value

            BoxWithConstraints {
                val height = this.maxHeight
                val density = LocalDensity.current

                val sheetState = rememberSwipeableSheetState(
                    initialValue = BottomSheetPosition.HIDDEN,
                    collapsedAnchorBottomPaddingPixels = 0f,
                    bottomScreenCornerRadius = vm.deviceApiProvider.deviceCornerRadius(),
                    confirmValueChange = {
                        it != BottomSheetPosition.HIDDEN
                    }
                )

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

                val sheetProgressTilCollapsed = sheetState.progress(
                    from = BottomSheetPosition.COLLAPSED,
                    to = BottomSheetPosition.HALF_EXPANDED
                ).coerceIn(0f, 1f)

                val sheetProgressTilMinimized = sheetState.progress(
                    from = BottomSheetPosition.COLLAPSED_MINIMISED,
                    to = BottomSheetPosition.COLLAPSED
                ).coerceIn(0f, 1f)

                val collapsedHeightDp = with(LocalDensity.current) {
                    sheetState.collapsedInfoHeightPixel.toDp()
                }

                var cardHeight by remember { mutableStateOf(0.dp) }

                LaunchedEffect(this.maxHeight, this.maxWidth, readyState?.mapHomeScreenFocusType?.asLocationHistory) {
                    vm.onEvent(MapHomeScreenUiEvent.OnScreenDensityChange(maxWidthDp = maxWidth, maxHeightDp = maxHeight))
                    vm.onEvent(LocationHistoryUiEvent.OnScreenDensityChange(maxWidthDp = maxWidth, maxHeightDp = maxHeight))
                }

                LaunchedEffect(key1 = Unit) {
                    vm.events.collectLatest { event ->
                        when (event) {
                            MapHomeScreenEvent.OpenGroupCreateScreen -> onCreateGroup()
                            MapHomeScreenEvent.OpenPaywall -> openPaywall()
                            is MapHomeScreenEvent.OpenCreatePlace -> onOpenCreatePlaceScreen(event.groupId, event.location)
                            is MapHomeScreenEvent.OpenGroupDetails -> onGroupDetailsClick(event.groupId)
                            is MapHomeScreenEvent.OpenGroupPicker -> onGroupPickerClick(event.showPersonalSpace)
                            is MapHomeScreenEvent.OpenPlaceDetails -> onPlaceClick(event)
                            is MapHomeScreenEvent.OpenPlaces -> onOpenPlacesScreen(event.groupId)
                            is MapHomeScreenEvent.OpenMemberInviteScreen -> onMemberInviteClick(event.groupId)
                            MapHomeScreenEvent.OpenSettings -> onSettingsClick()
                            is MapHomeScreenEvent.OpenSharingStart -> onSharingStartClick(event.groupId)
                            is MapHomeScreenEvent.OpenExternalNavigation -> onDirectionsRequest(event)
                            MapHomeScreenEvent.OpenPersonalSpace -> openPersonalSpace()
                            is MapHomeScreenEvent.ShowPlanLimitAlert -> openPaywall()
                            MapHomeScreenEvent.OpenMapProviderSettings -> {}
                            null -> {}
                            MapHomeScreenEvent.AskNotificationPermission -> askNotificationPermission()
                            MapHomeScreenEvent.OpenPersonalCodes -> openPersonalCodes()
                            is MapHomeScreenEvent.SharePersonalCode -> onSharePersonalCode(event.content)
                        }
                    }
                }

                LaunchedEffect(key1 = Unit) {
                    vm.historyEvents.collectLatest { event ->
                        logg.v("snackbar_paywall", "Got history event on map: $event")
                        when (event) {
                            LocationHistoryScreenEvent.NavigateBack -> vm.onEvent(MapHomeScreenUiEvent.CloseLocationHistory)
                            is LocationHistoryScreenEvent.OpenNavigation -> onDirectionsRequest(event.event)
                            LocationHistoryScreenEvent.OpenHistorySettings -> uiState.selectedGroupId?.let { onHistorySettingsClick(it) }
                            LocationHistoryScreenEvent.OpenPaywall -> openPaywall()
                            is LocationHistoryScreenEvent.OpenRawDataScreen -> openRawDataScreen(event.type)

                            null -> {}
                        }
                    }
                }

                LaunchedEffect(
                    bottomOffset,
                    cardHeight,
                    historyUiState != null
                ) {
                    snapshotFlow { bottomOffset to cardHeight }
                        .distinctUntilChanged()
                        .collectLatest {
                            val bottomOffset = if (readyState?.mapHomeScreenFocusType?.isMemberPlaceOrHistoryFocused == true) {
                                (cardHeight + paddingValues.bottom.dp).coerceIn(
                                    minimumValue = paddingValues.bottom.dp,
                                    maximumValue = height
                                )
                            } else {
                                val range = height * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
                                val padding = if (sheetProgressTilMinimized < 1f) {
                                    collapsedHeightDp.times(sheetProgressTilMinimized)
                                } else {
                                    collapsedHeightDp + (range - collapsedHeightDp).times(sheetProgressTilCollapsed)
                                }

                                padding.coerceIn(
                                    minimumValue = 0.dp,
                                    maximumValue = range
                                )
                            }
                            onSheetOffsetChange(bottomOffset.value)
                        }
                }

                val isTopHeaderVisible = if (historyUiState != null) {
                    !historyUiState.canCollapseMapHeader
                } else {
                    !uiState.canCollapseMapHeader
                }

                val mainDialog = uiState.alertDialog
                val historyDialog = historyUiState?.alertDialog
                mainDialog?.Show {
                    vm.onEvent(
                        MapHomeScreenUiEvent.DialogAction(
                            AlertDialogAction(dialog = mainDialog, action = it)
                        )
                    )
                }

                historyDialog?.Show {
                    vm.onEvent(
                        LocationHistoryUiEvent.DialogAction(
                            AlertDialogAction(dialog = historyDialog, action = it)
                        )
                    )
                }

                MainMapSheet(
                    appState = it,
                    screenPaddingValues = remember(paddingValues) {
                        paddingValues.copy(bottom = 0f)
                    }.toComposePaddingValues(),
                    swipeableState = sheetState,
                    mapUiState = uiState,
                    onEvent = vm::onEvent,
                    screenContent = {
                        UIKitViewController(
                            modifier = Modifier.fillMaxSize(),
                            factory = mapUIView,
                            properties = UIKitInteropProperties(
                                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                                isNativeAccessibilityEnabled = true
                            )
                        )

                        MapHomeScreenElements(
                            modifier = Modifier,
                            state = uiState,
                            isTopHeaderControlVisible = isTopHeaderVisible,
                            isActionRowControlVisible = sheetState.currentValue.order <= BottomSheetPosition.COLLAPSED.order,
                            screenPaddingValues = paddingValues.toComposePaddingValues(),
                            containerHeight = this.maxHeight,
                            bottomOffset = bottomOffset,
                            isSheetHidden = sheetState.currentValue == BottomSheetPosition.HIDDEN,
                            sheetFullProgress = sheetState.progress(
                                from = BottomSheetPosition.EXPANDED,
                                to = BottomSheetPosition.COLLAPSED
                            ),
                            onEvent = vm::onEvent,
                            bottomCards = { sheetPeekHeight ->
                                if (historyUiState != null) {
                                    LocationHistoryMapCardUi(
                                        uiState = historyUiState,
                                        isSmallScreenSize = LocalScreenDimensionType.current.maxHeightDp.value < 850,
                                        cardHeight = { cardHeight = it },
                                        onEvent = vm::onEvent
                                    )
                                } else {
                                    MapHomeElementMainCards(
                                        readyState = readyState,
                                        screenPaddingValues = paddingValues.toComposePaddingValues(),
                                        sheetPeekHeight = sheetPeekHeight,
                                        cardHeight = { cardHeight = it },
                                        onEvent = vm::onEvent,
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}