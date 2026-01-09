package app.paralino.android.ui.map

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.R
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun defaultGoogleMapUiSettings(): MutableState<MapUiSettings> {
    return remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                tiltGesturesEnabled = false
            )
        )
    }
}

@Composable
fun defaultGoogleMapProperties(
    isDarkMode: Boolean,
    satelliteMode: Boolean
): MutableState<MapProperties> {
    val context = LocalContext.current
    return remember(satelliteMode, isDarkMode) {
        mutableStateOf(
            MapProperties(
                mapType = if (satelliteMode) MapType.HYBRID else MapType.NORMAL,
                isBuildingEnabled = true,
                isIndoorEnabled = false,
                isMyLocationEnabled = false,
                isTrafficEnabled = false,
                maxZoomPreference = 19f,
                minZoomPreference = 3f,
                mapStyleOptions = if (isDarkMode) {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_dark_style)
                } else {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_light_style)
                }
            )
        )
    }
}

private fun boundsForMarkers(markers: List<MapPinData>, ignoreAccuracy: Boolean): LatLngBounds? {
    if (markers.isEmpty())
        return null
    return LatLngBounds.builder()
        .apply {
            markers.forEach {
                val distanceFromCenterToCorner = (if (ignoreAccuracy) 1f else it.accuracy) * Math.sqrt(2.0)
                val center = LatLng(it.lat, it.lng)
                val southwestCorner =
                    SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0)
                val northeastCorner =
                    SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0)
                include(southwestCorner)
                include(northeastCorner)
            }
        }
        .build()
}

fun CameraPositionState.refocus(
    scope: CoroutineScope,
    markers: List<MapPinData>,
    forceZoom: Boolean = true,
    ignoreAccuracy: Boolean = false,
    padding: Int = 60,
    durationMs: Int = 500
) {
    scope.launch {
        if (markers.isEmpty()) return@launch
        val bounds = boundsForMarkers(markers = markers, ignoreAccuracy = ignoreAccuracy) ?: return@launch

        val result = FloatArray(1)
        Location.distanceBetween(
            bounds.northeast.latitude,
            bounds.northeast.longitude,
            bounds.southwest.latitude,
            bounds.southwest.longitude,
            result
        )

        var cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        val currentBounds = this@refocus.projection?.visibleRegion?.latLngBounds
        currentBounds?.let {
            val fitBounds = currentBounds.canFitBounds(bounds)
            if (forceZoom) {
                if (fitBounds && result.first() < 500) {
                    cameraUpdate = CameraUpdateFactory.newLatLngZoom(bounds.center, 17f)
                }
            } else {
                if (fitBounds) {
                    cameraUpdate = CameraUpdateFactory.newLatLng(bounds.center)
                }
            }
        } ?: let {
            if (result.first() < 500) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(bounds.center, 17f)
            }
        }

        if (durationMs > 0) {
            this@refocus.animate(
                update = cameraUpdate,
                durationMs = durationMs
            )
        } else {
            this@refocus.move(update = cameraUpdate)
        }
    }
}

fun CameraPositionState.focusOnMap(
    scope: CoroutineScope,
    focusType: MapHomeScreenFocusType?,
    markers: List<MapPinData>,
    focusedMarker: FocusedMarkerData?,
    durationMs: Int
) {
    val focus = when (focusType) {
        is MapHomeScreenFocusType.PLACE,
        is MapHomeScreenFocusType.MEMBER -> {
            listOfNotNull(
                markers.firstOrNull { it.markerId == focusedMarker?.markerId }
            )
        }

        MapHomeScreenFocusType.ALL_PLACES -> {
            markers.filter { it.pinType == MapPinType.PLACE }
        }

        MapHomeScreenFocusType.ALL_MEMBERS -> {
            markers.filter { it.pinType == MapPinType.MEMBER }
        }

        else -> {
            emptyList()
        }
    }

    val padding = when (focusType) {
        MapHomeScreenFocusType.ALL_MEMBERS -> {
            if (markers.size == 1) 200 else 100
        }

        MapHomeScreenFocusType.ALL_PLACES -> {
            if (markers.size == 1) 300 else 100
        }

        else -> 100
    }

    val forceZoom = when (focusType) {
        is MapHomeScreenFocusType.MEMBER -> focusType.forceZoom
        else -> true
    }
    refocus(
        scope = scope,
        markers = focus,
        forceZoom = forceZoom,
        padding = 100,
        durationMs = durationMs
    )
}

private fun LatLngBounds.canFitBounds(
    bounds: LatLngBounds
): Boolean {

    val first = this
    val latDistance1 = SphericalUtil.computeDistanceBetween(
        first.southwest,
        // southeast
        LatLng(first.southwest.latitude, first.northeast.longitude)
    )
    val latDistance2 = SphericalUtil.computeDistanceBetween(
        bounds.southwest,
        // southeast
        LatLng(bounds.southwest.latitude, bounds.northeast.longitude)
    )

    val lngDistance1 = SphericalUtil.computeDistanceBetween(
        first.southwest,
        // northwest
        LatLng(first.northeast.latitude, first.southwest.longitude)
    )

    val lngDistance2 = SphericalUtil.computeDistanceBetween(
        bounds.southwest,
        // northwest
        LatLng(bounds.northeast.latitude, bounds.southwest.longitude)
    )

    return latDistance1 >= latDistance2 && lngDistance1 >= lngDistance2
}