package app.paralino.android.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.utils.Emojis
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun MapMarkerComposable(
    state: MapPinData,
    position: LatLng,
    onClick: (MapPinData) -> Unit
) {
    MarkerComposable(
        keys = listOfNotNull(
            state.active,
            state.focused,
            state.image,
            state.markerStyle
        ).toTypedArray(),
        state = MarkerState(position = position),
        tag = state.markerId,
        anchor = when (state.markerStyle) {
            MapMarkerStyle.PIN -> Offset(0.5f, 1f)
            MapMarkerStyle.CIRCLE -> Offset(0.5f, 0.5f)
        },
        onClick = {
            onClick(state)
            true
        }
    ) {
        Box(
            modifier = Modifier.size(state.getPinSize().dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (state.markerStyle == MapMarkerStyle.PIN) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.ic_pin_bubble),
                    contentDescription = null
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(0.9f),
                contentAlignment = Alignment.Center
            ) {
                ImageModelImage(
                    modifier = Modifier.fillMaxSize(0.9f),
                    imageModel = state.image ?: ImageModel.emoji(Emojis.lock),
                    clipShape = CircleShape
                )
            }
        }
    }
}

@Composable
fun defaultMapUiSettings(): MutableState<MapUiSettings> {
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
fun defaultMapProperties(
    isDarkMode: Boolean,
    satelliteMode: Boolean
): MutableState<MapProperties> {
    val context = LocalContext.current
    return remember(satelliteMode, isDarkMode) {
        mutableStateOf(
            MapProperties(
                mapType = if (satelliteMode) MapType.HYBRID else MapType.NORMAL,
                isBuildingEnabled = false,
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


fun boundsForMarkers(markers: List<MapPinData>): LatLngBounds? {
    if(markers.isEmpty())
        return null
    return LatLngBounds.builder()
        .apply {
            markers.forEach {
                val distanceFromCenterToCorner = it.accuracy * Math.sqrt(2.0)
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
    padding: Int = 60,
    durationMs: Int = 500
) {
    scope.launch {
        val bounds = boundsForMarkers(markers) ?: return@launch
        val update = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        if (durationMs > 0) {
            this@refocus.animate(
                update = update,
                durationMs = durationMs
            )
        } else {
            this@refocus.move(update = update)
        }
    }
}