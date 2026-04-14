package app.paralino.android.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.map.google.GoogleMapSimpleMapComposable
import app.paralino.android.ui.map.maplibre.MapLibreSimpleMapComposable
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.models.settings.MapsSDKProvider


@Composable
fun SimpleGenericMapComposable(
    modifier: Modifier = Modifier,
    mapsSDKProvider: MapsSDKProvider?,
    markers: List<MapPinData>,
    fallbackCenterLatLng: LatLng?,
    zoomLevel: Float,
    canTapOnCircles: Boolean,
    isSatelliteMode: Boolean,
    isSystemInDarkTheme: Boolean,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapInteraction: (Boolean) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {
    when (mapsSDKProvider) {
        MapsSDKProvider.GOOGLE -> {
            GoogleMapSimpleMapComposable(
                modifier = modifier,
                markers = markers,
                fallbackCenterLatLng = fallbackCenterLatLng,
                zoomLevel = zoomLevel,
                isSatelliteMode = isSatelliteMode,
                canTapOnCircles = canTapOnCircles,
                onMapCameraSettled = onMapCameraSettled,
                onMapInteraction = onMapInteraction,
                onMapTap = onMapTap,
                onMapLongPress = onMapLongPress
            )
        }

        is MapsSDKProvider.MAPLIBRE -> {
            MapLibreSimpleMapComposable(
                modifier = modifier,
                mapsSDKProvider = mapsSDKProvider,
                isSystemInDarkTheme = isSystemInDarkTheme,
                markers = markers,
                fallbackCenterLatLng = fallbackCenterLatLng,
                zoomLevel = zoomLevel.toDouble(),
                isSatelliteMode = isSatelliteMode,
                canTapOnCircles = canTapOnCircles,
                onMapCameraSettled = onMapCameraSettled,
                onMapInteraction = onMapInteraction,
                onMapTap = onMapTap,
                onMapLongPress = onMapLongPress
            )
        }

        null -> {}
    }
}