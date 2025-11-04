package app.paralino.android.ui.map

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.history.LocationHistoryMapContent
import app.paralino.android.ui.history.rememberLocationHistoryMapController
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.history.LocationHistoryMapCardUi
import app.paralino.core.ui.map.BottomSheetPosition
import app.paralino.core.ui.map.MapHomeElementMainCards
import app.paralino.core.ui.map.MapHomeScreenElements
import app.paralino.core.ui.map.SwipeableSheetState
import com.google.maps.android.compose.rememberCameraPositionState
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
internal fun HomeScreenMapWithElements(
    appState: MainAppState,
    uiState: MapHomeScreenState,
    historyUiState: LocationHistoryScreenUiState?,
    sheetState: SwipeableSheetState,
    readyState: MapHomeScreenState.Ready?,
    bottomOffset: Dp,
    onMainEvent: (MapHomeScreenUiEvent) -> Unit,
    onHistoryEvent: (LocationHistoryUiEvent) -> Unit,
) {
    var cardHeight by remember { mutableStateOf(0.dp) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val screenAreaPadding = WindowInsets.safeDrawing.asPaddingValues()
        val bottomPadding = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
        val mapContentPadding by remember(
            screenAreaPadding,
            bottomPadding,
            readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused,
            historyUiState != null,
            bottomOffset,
            cardHeight
        ) {
            derivedStateOf {
                PaddingValues(
                    top = screenAreaPadding.calculateTopPadding() + 64.dp,
                    bottom = if (readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true || historyUiState != null) {
                        (cardHeight + bottomPadding).coerceIn(
                            minimumValue = bottomPadding,
                            maximumValue = this.maxHeight
                        )
                    } else {
                        (this.maxHeight - bottomOffset).coerceIn(
                            minimumValue = bottomPadding,
                            maximumValue = this.maxHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
                        )
                    }
                )
            }
        }

        var isMapLoaded by remember { mutableStateOf(false) }

        val cameraPositionState = rememberCameraPositionState()
        val homeMapController = rememberMainHomeMapController(
            uiState = uiState,
            mapContentPadding = mapContentPadding,
            isMapLoaded = isMapLoaded,
            cameraPositionState = cameraPositionState,
            onMapTouch = {
                onMainEvent(MapHomeScreenUiEvent.OnMapTouch)
            }
        )

        val mapController = if (historyUiState != null) {
            rememberLocationHistoryMapController(
                uiState = historyUiState,
                mapContentPadding = mapContentPadding,
                cameraPositionState = cameraPositionState,
                animateFirstContent = true,
                isSatelliteMode = uiState.isSatelliteMode,
                onMapTouch = {
                    onHistoryEvent(LocationHistoryUiEvent.OnMapTouch)
                }
            )
        } else {
            homeMapController
        }

        GenericMapWrapper(
            modifier = Modifier,
            mapData = mapController,
            onMapClick = {
                if (historyUiState != null) {
                    onHistoryEvent(LocationHistoryUiEvent.OnMapTouch)
                } else {
                    onMainEvent(MapHomeScreenUiEvent.OnMapTouch)
                }
            },
            onMapLongClick = {
                val groupId = uiState.asReady()?.groupOrSpaceId?.id
                if (groupId != null) {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceCreate.route(
                            groupId = groupId,
                            placeId = null,
                            lat = it.lat,
                            lng = it.lng
                        )
                    )
                }
            },
            onMapLoaded = {
                isMapLoaded = true
            },
            content = {
                if (historyUiState != null) {
                    LocationHistoryMapContent(
                        uiState = historyUiState,
                        onEvent = onHistoryEvent
                    )
                } else {
                    MainHomeMapContent(
                        uiState = readyState,
                        onEvent = onMainEvent
                    )
                }
            }
        )

        val screenPadding = WindowInsets.safeDrawing.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val screenPaddingValues = remember {
            PaddingValues(
                top = screenPadding.calculateTopPadding() + 4.dp,
                bottom = screenPadding.calculateBottomPadding(),
                start = screenPadding.calculateStartPadding(layoutDirection),
                end = screenPadding.calculateEndPadding(layoutDirection),
            )
        }

        MapHomeScreenElements(
            modifier = Modifier,
            state = uiState,
            screenPaddingValues = screenPaddingValues,
            containerHeight = this.maxHeight,
            bottomOffset = bottomOffset,
            isSheetHidden = sheetState.currentValue == BottomSheetPosition.HIDDEN,
            sheetFullProgress = sheetState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.EXPANDED
            ),
            onEvent = onMainEvent,
            bottomCards = { sheetPeekHeight ->
                if (historyUiState != null) {
                    LocationHistoryMapCardUi(
                        uiState = historyUiState,
                        cardHeight = { cardHeight = it },
                        onEvent = onHistoryEvent
                    )
                } else {
                    MapHomeElementMainCards(
                        readyState = readyState,
                        screenPaddingValues = screenPaddingValues,
                        sheetPeekHeight = sheetPeekHeight,
                        cardHeight = { cardHeight = it },
                        onEvent = onMainEvent,
                    )
                }
            }
        )
    }
}