package app.paralino.android.ui.map.maplibre

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.GenericMapLoadingStatePlaceholder
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.settings.MapsSDKProvider
import kotlinx.coroutines.flow.first
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.LocalMapState
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.AttributionDefaults
import org.maplibre.compose.overlay.AttributionDefaults.contentDescription
import org.maplibre.compose.overlay.AttributionLinks
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.overlay.attributions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position

@Composable
fun rememberMapLibreMapState(
    mapsSDKProvider: MapsSDKProvider?,
    isSatelliteMode: Boolean,
    isSystemInDarkTheme: Boolean,
    content: @Composable @MaplibreComposable () -> Unit,
): MapState {
    val mapProvider = remember(mapsSDKProvider) {
        (mapsSDKProvider as? MapsSDKProvider.MAPLIBRE)
            ?: MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE()
    }

    val mapStyle = if (isSystemInDarkTheme) {
        BaseStyle.Uri(mapProvider.darkStyleUri(isSatelliteMaps = isSatelliteMode))
    } else {
        BaseStyle.Uri(mapProvider.lightStyleUri(isSatelliteMaps = isSatelliteMode))
    }

    return rememberMapState(baseStyle = mapStyle, content = content)
}

@Composable
fun GenericMapLibreMapWrapper(
    modifier: Modifier = Modifier,
    mapData: MapLibreMapUiControlledData,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    onMapAttributionExpanded: (Boolean) -> Unit,
    placeholder: @Composable BoxScope.() -> Unit = {
        GenericMapLoadingStatePlaceholder(modifier = Modifier.fillMaxSize())
    },
) {
    var mapOverlayShown by rememberSaveable { mutableStateOf(true) }

    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
    val currentOnMapLoaded by rememberUpdatedState(onMapLoaded)

    val interactions = remember {
        MapInteractions {
            callbacks {
                click {
                    onEvent { event ->
                        event.position?.let { currentOnMapClick(it.toLatLng()) }
                        ClickResult.Pass
                    }
                }
                longClick {
                    onEvent { event ->
                        event.position?.let { currentOnMapLongClick(it.toLatLng()) }
                        ClickResult.Pass
                    }
                }
            }
        }
    }

    // Style load state does not wait when tiles are loaded, so we block until first idle event
    // this is when map is drawn
    LaunchedEffect(mapData.mapState) {
        mapData.mapState.events.first { it is MapEvent.Idle }
        currentOnMapLoaded()
        mapOverlayShown = false
    }

    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        state = mapData.mapState,
        interactions = interactions,
        overlay = {
            MapLibreMapOverlayContent(
                overlayPadding = PaddingValues(
                    top = mapData.contentPadding.calculateTopPadding(),
                    bottom = mapData.contentPadding.calculateBottomPadding().minus(6.dp)
                        .coerceAtLeast(0.dp),
                    start = mapData.contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = mapData.contentPadding.calculateEndPadding(LayoutDirection.Ltr),
                ),
                isCompact = mapData.isCompactAttribution,
                onMapAttributionExpanded = onMapAttributionExpanded
            )
        }
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
private fun MapLibreMapOverlayContent(
    overlayPadding: PaddingValues,
    isCompact: Boolean,
    onMapAttributionExpanded: (Boolean) -> Unit
) {
    val mapState = checkNotNull(LocalMapState.current)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(overlayPadding)
    ) {
        DisappearingCompassButton(
            modifier = Modifier.align(Alignment.TopStart),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = if (isCompact) 0.dp else 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(mapState.isCameraMoving, mapState.cameraMoveReason) {
                if (mapState.isCameraMoving && mapState.cameraMoveReason == CameraMoveReason.Gesture) {
                    expanded = false
                    onMapAttributionExpanded(false)
                }
            }

            MapLibreAttributionButton(
                mapState = mapState,
                expanded = expanded,
                isCompact = isCompact,
                onClick = {
                    onMapAttributionExpanded(!expanded)
                    expanded = !expanded
                }
            )

            MaplibreLogo(
                modifier = Modifier.height(height = if (isCompact) 18.dp else 22.dp),
            )
        }
    }
}

@Composable
private fun MapLibreAttributionButton(
    mapState: MapState,
    expanded: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val attributions by remember(mapState) { derivedStateOf { mapState.style.attributions() } }

    val attributionStyle = if (expanded) {
        AttributionDefaults.expandedStyle()
    } else {
        AttributionDefaults.collapsedStyle()
    }
    val containerColor by animateColorAsState(attributionStyle.containerColor)
    val contentColor by animateColorAsState(attributionStyle.contentColor)

    Row(
        modifier = Modifier
            .background(containerColor, attributionStyle.shape)
            .clip(attributionStyle.shape)
            // Without this, a drag across the attribution pans the map underneath it.
            .pointerInput(Unit) {}
            .semantics { isTraversalGroup = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = AttributionDefaults.expand(Alignment.CenterEnd),
            exit = AttributionDefaults.collapse(Alignment.CenterEnd),
        ) {
            AttributionLinks(
                attributions = attributions,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                textStyle = attributionStyle.textStyle.copy(color = contentColor),
            )
        }

        Box(
            Modifier
                .size(if (isCompact) 26.dp else 32.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onClick, role = Role.Button),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = AttributionDefaults.icon(),
                contentDescription = contentDescription(),
                modifier = Modifier.size(if (isCompact) 18.dp else 22.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSurface.copy(alpha = 0.55f)),
            )
        }
    }
}

// Since 0.16, map clicks report the tapped world copy (new world copy is created when panning map),
// so longitude can fall outside of ±180 after panning across the antimeridian
private fun Position.toLatLng(): LatLng {
    val wrappedLongitude = if (longitude in -180.0..180.0) {
        longitude
    } else {
        (longitude + 180.0).mod(360.0) - 180.0
    }
    return LatLng(lat = latitude, lng = wrappedLongitude)
}

@Stable
class MapLibreMapUiControlledData(
    val mapState: MapState,
    val contentPadding: PaddingValues,
    val isCompactAttribution: Boolean
)