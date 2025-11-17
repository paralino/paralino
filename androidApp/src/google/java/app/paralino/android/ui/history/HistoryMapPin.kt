package app.paralino.android.ui.history

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.rememberComposeBitmapDescriptor
import app.paralino.core.presentation.history.LocationHistoryPointState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
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
        anchor = Offset(0.5f, 1f),
        onClick = {
            onClick(state)
            true
        },
        infoWindowAnchor = Offset(0.5f, 0f),
        icon = mapMarkerIcon(
            isDarkMode = isDarkMode,
            isAccurate = !state.accuracy.isLocationInaccurate()
        ),
        zIndex = zIndex,
        content = null
    )
}

@Composable
private fun mapMarkerIcon(
    isDarkMode: Boolean,
    isAccurate: Boolean,
): BitmapDescriptor {
    return rememberComposeBitmapDescriptor(
        isDarkMode, isAccurate
    ) {
        Icon(
            modifier = Modifier
                .size(
                    if (!isAccurate) {
                        18.dp
                    } else {
                        16.dp
                    }
                ),
            painter = if (!isAccurate) {
                CoreAppIcons.NotListedLocationFilled.asPainterResource()
            } else {
                CoreAppIcons.LocationOnFilled.asPainterResource()
            },
            contentDescription = null,
            tint = if (isDarkMode) {
                AppColors.highlightGold
            } else {
                AppColors.redMain
            }
        )
    }
}

class LocationHistoryClusterItem(
    val item: LocationHistoryPointState
): ClusterItem {
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