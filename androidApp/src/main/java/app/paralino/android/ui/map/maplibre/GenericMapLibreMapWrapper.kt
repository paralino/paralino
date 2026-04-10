package app.paralino.android.ui.map.maplibre

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.paralino.android.ui.map.GenericMapLoadingStatePlaceholder
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.settings.generic.MapsSDKProvider
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable

@Composable
fun GenericMapLibreMapWrapper(
    modifier: Modifier = Modifier,
    mapData: MapLibreMapUiControlledData,
    isSystemInDarkTheme: Boolean,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    placeholder: @Composable @MaplibreComposable BoxScope.() -> Unit = {
        GenericMapLoadingStatePlaceholder(modifier = Modifier.fillMaxSize())
    },
    content: @Composable () -> Unit,
) {
    var mapOverlayShown by rememberSaveable { mutableStateOf(true) }

    val mapProvider = remember(mapData.mapsSDKProvider) {
        (mapData.mapsSDKProvider as? MapsSDKProvider.MAPLIBRE) ?: MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE()
    }

    val mapStyle = if (isSystemInDarkTheme) {
        BaseStyle.Uri(mapProvider.darkStyleUri(isSatelliteMaps = mapData.isSatelliteMode))
    } else {
        BaseStyle.Uri(mapProvider.lightStyleUri(isSatelliteMaps = mapData.isSatelliteMode))
    }

    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        cameraState = mapData.cameraState,
        options = mapData.options,
        baseStyle = mapStyle,
        onMapClick = { position, _ ->
            onMapClick(LatLng(lat = position.latitude, lng = position.longitude))
            ClickResult.Pass
        },
        onMapLongClick = { position, _ ->
            onMapLongClick(LatLng(lat = position.latitude, lng = position.longitude))
            ClickResult.Pass
        },
        onMapLoadFinished = {
            onMapLoaded()
            mapOverlayShown = false
        },
        content = content
    )

    AnimatedVisibility(
        visible = mapOverlayShown,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onMapLoaded()
                        mapOverlayShown = false
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            placeholder()
        }
    }
}

@Stable
class MapLibreMapUiControlledData(
    val options: MapOptions,
    val cameraState: CameraState,
    val isSatelliteMode: Boolean,
    val contentPadding: PaddingValues,
    val mapsSDKProvider: MapsSDKProvider?
)