package app.paralino.android.ui.map

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
import app.paralino.core.models.location.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings

@Composable
fun GenericGoogleMapWrapper(
    modifier: Modifier = Modifier,
    mapData: GoogleMapUiControlledData,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    placeholder: @Composable BoxScope.() -> Unit = {
        GenericMapLoadingStatePlaceholder(modifier = Modifier.fillMaxSize())
    },
    content: @Composable @GoogleMapComposable () -> Unit,
) {
    var mapOverlayShown by rememberSaveable { mutableStateOf(true) }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = mapData.cameraPositionState,
        properties = mapData.properties,
        uiSettings = mapData.uiSettings,
        contentPadding = mapData.contentPadding,
        onMapLongClick = {
            onMapLongClick(
                LatLng(
                    lat = it.latitude,
                    lng = it.longitude
                )
            )
        },
        onMapClick = {
            onMapClick(
                LatLng(
                    lat = it.latitude,
                    lng = it.longitude
                )
            )
        },
        onMapLoaded = {
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
class GoogleMapUiControlledData(
    val cameraPositionState: CameraPositionState,
    val properties: MapProperties,
    val uiSettings: MapUiSettings,
    val contentPadding: PaddingValues
)