package app.paralino.android.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.map.google.GoogleMapBasePlaceMapComposable
import app.paralino.android.ui.map.maplibre.MapLibreBasePlaceMapComposable
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.create.PlaceMapFallbackLatLng
import app.paralino.core.presentation.settings.generic.MapsSDKProvider


@Composable
fun MapPlaceComposable(
    modifier: Modifier = Modifier,
    mapsSDKProvider: MapsSDKProvider?,
    markers: List<MapPinData>,
    fallbackCenterLatLng: PlaceMapFallbackLatLng?,
    canTapOnCircles: Boolean,
    isSatelliteMode: Boolean,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapInteraction: (Boolean) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {
    when (mapsSDKProvider) {
        MapsSDKProvider.GOOGLE -> {
            GoogleMapBasePlaceMapComposable(
                modifier = modifier,
                markers = markers,
                fallbackCenterLatLng = fallbackCenterLatLng,
                isSatelliteMode = isSatelliteMode,
                canTapOnCircles = canTapOnCircles,
                onMapCameraSettled = onMapCameraSettled,
                onMapInteraction = onMapInteraction,
                onMapTap = onMapTap,
                onMapLongPress = onMapLongPress
            )
        }

        MapsSDKProvider.MAPLIBRE_FREE,
        MapsSDKProvider.MAPLIBRE_PREMIUM -> {
            MapLibreBasePlaceMapComposable(
                modifier = modifier,
                mapsSDKProvider = mapsSDKProvider,
                markers = markers,
                fallbackCenterLatLng = fallbackCenterLatLng,
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