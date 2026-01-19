package app.paralino.android.ui.map.google.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.google.rememberComposeBitmapDescriptor
import app.paralino.core.presentation.history.LocationHistoryPointState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.conditional
import app.paralino.core.utils.extensions.isLocationInaccurate
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.rememberUpdatedMarkerState


@Composable
fun HistoryMapPin(
    state: LocationHistoryPointState,
    isDarkMode: Boolean,
    position: LatLng,
    zIndex: Float,
    onClick: (LocationHistoryPointState) -> Unit
) {
    val markerState = rememberUpdatedMarkerState(position = position)

    MarkerInfoWindow(
        state = markerState,
        tag = state.pointId,
        anchor = Offset(0.5f, if (state is LocationHistoryPointState.Cluster) 0.5f else 1f),
        onClick = {
            onClick(state)
            true
        },
        infoWindowAnchor = Offset(0.5f, 0f),
        icon = mapMarkerIcon(
            isDarkMode = isDarkMode,
            isAccurate = !state.accuracy.isLocationInaccurate(),
            isClustered = state is LocationHistoryPointState.Cluster
        ),
        zIndex = zIndex,
        content = null
    )
}

@Composable
private fun mapMarkerIcon(
    isDarkMode: Boolean,
    isAccurate: Boolean,
    isClustered: Boolean
): BitmapDescriptor {
    return rememberComposeBitmapDescriptor(
        isDarkMode, isAccurate, isClustered
    ) {
        HistoryMapIcon(
            isDarkMode = isDarkMode,
            isAccurate = isAccurate,
            isClustered = isClustered
        )
    }
}

@Composable
fun HistoryMapIcon(
    isDarkMode: Boolean,
    isAccurate: Boolean,
    isClustered: Boolean
) {
    Icon(
        modifier = Modifier
            .size(
                if (isClustered) {
                    24.dp
                } else if (isAccurate) {
                    16.dp
                } else {
                    28.dp
                }
            )
            .conditional(isClustered) {
                background(
                    color = AppColors.lightSurface,
                    shape = CircleShape
                )
            },
        painter = if (isClustered) {
            CoreAppIcons.CircleCircle.asPainterResource()
        } else if (isAccurate) {
            CoreAppIcons.LocationOnFilled.asPainterResource()
        } else {
            CoreAppIcons.NotListedLocationFilled.asPainterResource()
        },
        contentDescription = null,
        tint = if (isClustered) {
            AppColors.redMain
        } else {
            Color.Unspecified
        }
    )
}

class LocationHistoryClusterItem(
    val item: LocationHistoryPointState
): ClusterItem {
    fun getAnchor(): Offset {
        return Offset(
            x = 0.5f,
            y = if (item is LocationHistoryPointState.Cluster) 0.5f else 1f
        )
    }

    override fun getPosition(): LatLng {
        return LatLng(item.lat!!, item.lng!!)
    }

    override fun getTitle(): String? {
        return null
    }

    override fun getSnippet(): String? {
        return null
    }

    override fun getZIndex(): Float? {
        return null
    }
}