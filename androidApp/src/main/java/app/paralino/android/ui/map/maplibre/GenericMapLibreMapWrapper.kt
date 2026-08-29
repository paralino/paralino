package app.paralino.android.ui.map.maplibre

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.GenericMapLoadingStatePlaceholder
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.settings.MapsSDKProvider
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.AttributionDefaults
import org.maplibre.compose.overlay.AttributionDefaults.contentDescription
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.overlay.ExpandingAttributionButton
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.MapOverlayScope
import org.maplibre.compose.overlay.MaplibreLogo
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
    onMapAttributionExpanded: (Boolean) -> Unit,
    placeholder: @Composable @MaplibreComposable BoxScope.() -> Unit = {
        GenericMapLoadingStatePlaceholder(modifier = Modifier.fillMaxSize())
    },
    content: @Composable () -> Unit,
) {
    var mapOverlayShown by rememberSaveable { mutableStateOf(true) }

    val mapProvider = remember(mapData.mapsSDKProvider) {
        (mapData.mapsSDKProvider as? MapsSDKProvider.MAPLIBRE)
            ?: MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE()
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
        contentWindowInsets = WindowInsets(),
        overlay = remember(mapData.contentPadding) {
            MapOverlay {
                MapLibreMapOverlayContent(
                    overlayPadding = PaddingValues(
                        top = mapData.contentPadding.calculateTopPadding(),
                        bottom = mapData.contentPadding.calculateBottomPadding().minus(6.dp)
                            .coerceAtLeast(0.dp),
                        start = mapData.contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = mapData.contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                    ),
                    onMapAttributionExpanded = onMapAttributionExpanded
                )
            }
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

@Composable
private fun MapOverlayScope.MapLibreMapOverlayContent(
    overlayPadding: PaddingValues,
    onMapAttributionExpanded: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(overlayPadding)
    ) {
        DisappearingCompassButton(
            cameraState = this@MapLibreMapOverlayContent.cameraState,
            modifier = Modifier.align(Alignment.TopStart),
        )

        val camera = this@MapLibreMapOverlayContent.cameraState
        val style = this@MapLibreMapOverlayContent.styleState
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(camera.isCameraMoving, camera.moveReason) {
                if (camera.isCameraMoving && camera.moveReason == CameraMoveReason.GESTURE) {
                    expanded = false
                    onMapAttributionExpanded(false)
                }
            }

            ExpandingAttributionButton(
                expanded = expanded,
                onClick = {
                    onMapAttributionExpanded(!expanded)
                    expanded = !expanded
                },
                styleState = style,
                toggleButton = {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(
                                onClick = {
                                    onMapAttributionExpanded(!expanded)
                                    expanded = !expanded
                                },
                                role = Role.Button
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = AttributionDefaults.icon(),
                            contentDescription = contentDescription(),
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface.copy(alpha = 0.55f)),
                        )
                    }
                }
            )

            MaplibreLogo()
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