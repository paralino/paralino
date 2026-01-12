package app.paralino.android.ui.map.maplibre

import android.location.Location
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.util.VisibleRegion
import org.maplibre.spatialk.geojson.Position
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds


fun boundsForMarkers(markers: List<MapPinData>, ignoreAccuracy: Boolean): LatLngBounds? {
    if (markers.isEmpty())
        return null
    return LatLngBounds.Builder()
        .apply {
            markers.forEach {
                val distanceFromCenterToCorner = (if (ignoreAccuracy) 1f else it.accuracy) * Math.sqrt(2.0)
                val center = LatLng(it.lat, it.lng)
                val southwestCorner =
                    MapLibreMapUtils.computeOffset(center, distanceFromCenterToCorner, 225.0)
                val northeastCorner =
                    MapLibreMapUtils.computeOffset(center, distanceFromCenterToCorner, 45.0)
                include(southwestCorner)
                include(northeastCorner)
            }
        }
        .build()
}

fun CameraState.refocus(
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
            bounds.northEast.latitude,
            bounds.northEast.longitude,
            bounds.southWest.latitude,
            bounds.southWest.longitude,
            result
        )

        val currentBounds = this@refocus.projection?.queryVisibleRegion()
        val zoom = currentBounds?.let {
            val fitBounds = currentBounds.canFitBounds(bounds)
            if (forceZoom) {
                if (fitBounds && result.first() < 500) {
                    17.0
                } else {
                    null
                }
            } else {
                if (fitBounds) {
                    -(1.0)
                } else {
                    null
                }
            }
        } ?: let {
            if (result.first() < 500) {
                17.0
            } else {
                null
            }
        }

        val position = bounds.center
        val newPosition = this@refocus.position.copy(
            target = Position(longitude = position.longitude, latitude = position.latitude, altitude = position.altitude),
            padding = if (zoom != null) PaddingValues(padding.dp) else this@refocus.position.padding,
            zoom = if (zoom != null && zoom > 0.0) zoom else this@refocus.position.zoom
        )
        if (durationMs > 0) {
            this@refocus.animateTo(
                finalPosition = newPosition,
                duration = durationMs.milliseconds
            )
        } else {
            this@refocus.position = newPosition
        }
    }
}

fun CameraState.focusOnMap(
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

private fun VisibleRegion.canFitBounds(
    bounds: LatLngBounds
): Boolean {
    val thisBounds = LatLngBounds.from(
        latNorth = this.farLeft.latitude,
        lonEast = this.farRight.longitude,
        latSouth = this.nearRight.latitude,
        lonWest = this.farLeft.longitude
    )
    return thisBounds.contains(bounds.southWest) && bounds.contains(thisBounds.northEast)
}

internal object MapLibreMapUtils {
    fun computeOffset(
        from: LatLng,
        distance: Double,
        heading: Double
    ): LatLng {
        var distance = distance
        var heading = heading
        distance /= 6371009
        heading = Math.toRadians(heading)
        // http://williams.best.vwh.net/avform.htm#LL
        val fromLat = Math.toRadians(from.latitude)
        val fromLng = Math.toRadians(from.longitude)
        val cosDistance = cos(distance)
        val sinDistance = sin(distance)
        val sinFromLat = sin(fromLat)
        val cosFromLat = cos(fromLat)
        val sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * cos(heading)
        val dLng = atan2(
            sinDistance * cosFromLat * sin(heading),
            cosDistance - sinFromLat * sinLat
        )
        return LatLng(
            Math.toDegrees(asin(sinLat)),
            Math.toDegrees(fromLng + dLng)
        )
    }
}