package app.paralino.android.ui.map.google

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent


/**
 * No-op Google Map composable wrapper to satisfy compiler
 */
@Composable
fun GoogleMapHomeScreenMapComposable(
    modifier: Modifier = Modifier,
    uiState: app.paralino.core.presentation.map.MapHomeScreenState,
    readyState: MapHomeScreenState.Ready?,
    historyUiState: LocationHistoryScreenUiState?,
    mapContentPadding: PaddingValues,
    onMainEvent: (MapHomeScreenUiEvent) -> Unit,
    onHistoryEvent: (LocationHistoryUiEvent) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text("N/A")
    }
}