package app.paralino.android.ui.map.google.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import app.paralino.android.ui.map.google.rememberComposeBitmapDescriptor
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.ui.map.HistoryMapIcon
import app.paralino.core.ui.map.StartStopHistoryMapIcon
import app.paralino.core.utils.extensions.isLocationInaccurate
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.rememberUpdatedMarkerState


@Composable
fun GoogleMapHistoryMapPin(
    state: LocationHistoryPointState,
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
            isAccurate = !state.accuracy.isLocationInaccurate(),
            isClustered = state is LocationHistoryPointState.Cluster
        ),
        zIndex = zIndex,
        content = null
    )
}

@Composable
fun GoogleMapHistoryStartStopMapPin(
    pointId: String,
    content: String,
    position: LatLng,
    zIndex: Float,
    onClick: (String) -> Unit
) {
    val markerState = rememberUpdatedMarkerState(position = position)

    MarkerInfoWindow(
        state = markerState,
        tag = pointId,
        anchor = Offset(0.5f, 0.5f),
        onClick = {
            onClick(pointId)
            true
        },
        infoWindowAnchor = Offset(0.5f, 0f),
        icon = startStopMarkerIcon(content = content),
        zIndex = zIndex,
        content = null
    )
}

@Composable
private fun mapMarkerIcon(
    isAccurate: Boolean,
    isClustered: Boolean
): BitmapDescriptor? {
    return rememberComposeBitmapDescriptor(
        isAccurate, isClustered
    ) {
        HistoryMapIcon(
            isAccurate = isAccurate,
            isClustered = isClustered
        )
    }
}

@Composable
private fun startStopMarkerIcon(content: String): BitmapDescriptor? {
    return rememberComposeBitmapDescriptor(content) {
        StartStopHistoryMapIcon(content = content)
    }
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