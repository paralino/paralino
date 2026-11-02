package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import app.paralino.core.presentation.map.MapTappedCoordinateState
import app.paralino.core.ui.components.MapActionBubbleMarkerContent
import kotlinx.coroutines.flow.collectLatest
import org.maplibre.compose.map.MapState
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapLibreTapActionOverlay(
    modifier: Modifier = Modifier,
    tappedState: MapTappedCoordinateState,
    mapState: MapState,
    showPrivacyZoneButton: Boolean,
    onCreatePlace: () -> Unit,
    onCreatePrivacyZone: () -> Unit,
) {
    var anchorOffset by remember { mutableStateOf<IntOffset?>(null) }
    val density = LocalDensity.current

    LaunchedEffect(tappedState, mapState) {
        snapshotFlow { mapState.cameraPosition }
            .collectLatest {
                val screenOffset = mapState.screenLocationFromPosition(
                    Position(
                        latitude = tappedState.lat,
                        longitude = tappedState.lng
                    )
                ) ?: return@collectLatest

                val screenPoint = with(density) {
                    IntOffset(
                        x = screenOffset.x.roundToPx(),
                        y = screenOffset.y.roundToPx()
                    )
                }

                anchorOffset = IntOffset(
                    x = screenPoint.x,
                    y = screenPoint.y
                )
            }
    }

    val offset = anchorOffset ?: return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        MapActionBubbleMarkerContent(
            modifier = Modifier,
            positionOnScreen = offset,
            showPrivacyZoneButton = showPrivacyZoneButton,
            onCreatePlace = onCreatePlace,
            onCreatePrivacyZone = onCreatePrivacyZone,
        )
    }
}
