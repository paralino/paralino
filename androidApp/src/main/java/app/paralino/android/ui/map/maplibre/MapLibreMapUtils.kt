package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
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
import org.maplibre.spatialk.geojson.BoundingBox
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
    padding: PaddingValues,
    durationMs: Int = 500
) {
    scope.launch {
        if (markers.isEmpty()) return@launch
        val bounds = boundsForMarkers(markers = markers, ignoreAccuracy = ignoreAccuracy) ?: return@launch

        val box = BoundingBox(
            southwest = Position(
                longitude = bounds.southWest.longitude, latitude = bounds.southWest.latitude, altitude = bounds.southWest.altitude
            ),
            northeast = Position(
                longitude = bounds.northEast.longitude, latitude = bounds.northEast.latitude, altitude = bounds.northEast.altitude
            )
        )
        // TODO add forceZoom

        val effectiveDuration = durationMs.coerceAtLeast(1).milliseconds
        this@refocus.animateTo(
            boundingBox = box,
            padding = padding,
            duration = effectiveDuration
        )
    }
}

fun CameraState.focusOnMap(
    scope: CoroutineScope,
    focusType: MapHomeScreenFocusType?,
    markers: List<MapPinData>,
    focusedMarker: FocusedMarkerData?,
    durationMs: Int,
    mapContentPadding: PaddingValues
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
            if (markers.size == 1) 60 else 80
        }

        MapHomeScreenFocusType.ALL_PLACES -> {
            if (markers.size == 1) 40 else 60
        }

        else -> 40
    }

    val forceZoom = when (focusType) {
        is MapHomeScreenFocusType.MEMBER -> focusType.forceZoom
        else -> true
    }
    refocus(
        scope = scope,
        markers = focus,
        forceZoom = forceZoom,
        padding = PaddingValues(
            start = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr) + padding.dp,
            top = mapContentPadding.calculateTopPadding() + padding.dp,
            end = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr) + padding.dp,
            bottom = mapContentPadding.calculateBottomPadding() + padding.dp
        ),
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