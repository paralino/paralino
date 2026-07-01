package app.paralino.android.ui.map.google

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
import androidx.compose.ui.unit.IntOffset
import app.paralino.core.presentation.map.MapTappedCoordinateState
import app.paralino.core.ui.components.MapActionBubbleMarkerContent
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GoogleMapTapActionOverlay(
    modifier: Modifier = Modifier,
    tappedState: MapTappedCoordinateState,
    cameraPositionState: CameraPositionState,
    showPrivacyZoneButton: Boolean,
    onCreatePlace: () -> Unit,
    onCreatePrivacyZone: () -> Unit,
) {
    var anchorOffset by remember { mutableStateOf<IntOffset?>(null) }

    LaunchedEffect(tappedState, cameraPositionState) {
        snapshotFlow { cameraPositionState.position }
            .collectLatest {
                val projection = cameraPositionState.projection ?: return@collectLatest
                val screenPoint = projection.toScreenLocation(
                    com.google.android.gms.maps.model.LatLng(tappedState.lat, tappedState.lng)
                )

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
