package app.paralino.android.ui.map.google

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.create.PlaceMapFallbackLatLng


/**
 * No-op Google Map composable wrapper to satisfy compiler
 */
@Composable
fun GoogleMapSimpleMapComposable(
    modifier: Modifier = Modifier,
    markers: List<MapPinData>,
    fallbackCenterLatLng: PlaceMapFallbackLatLng?,
    canTapOnCircles: Boolean,
    isSatelliteMode: Boolean,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapInteraction: (Boolean) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text("N/A")
    }
}
