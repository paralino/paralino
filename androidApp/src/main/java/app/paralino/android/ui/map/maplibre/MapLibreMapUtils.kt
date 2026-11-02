package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.toDpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds


fun boundsForMarkers(markers: List<MapPinData>, ignoreAccuracy: Boolean): BoundingBox? {
    if (markers.isEmpty())
        return null

    var minLat = Double.POSITIVE_INFINITY
    var minLng = Double.POSITIVE_INFINITY
    var maxLat = Double.NEGATIVE_INFINITY
    var maxLng = Double.NEGATIVE_INFINITY

    markers.forEach {
        val distanceFromCenterToCorner = (if (ignoreAccuracy) 1f else it.accuracy) * Math.sqrt(2.0)
        val center = LatLng(lat = it.lat, lng = it.lng)
        val southwestCorner =
            MapLibreMapUtils.computeOffset(center, distanceFromCenterToCorner, 225.0)
        val northeastCorner =
            MapLibreMapUtils.computeOffset(center, distanceFromCenterToCorner, 45.0)
        minLat = minOf(minLat, southwestCorner.lat, northeastCorner.lat)
        maxLat = maxOf(maxLat, southwestCorner.lat, northeastCorner.lat)
        minLng = minOf(minLng, southwestCorner.lng, northeastCorner.lng)
        maxLng = maxOf(maxLng, southwestCorner.lng, northeastCorner.lng)
    }

    return BoundingBox(
        west = minLng,
        south = minLat,
        east = maxLng,
        north = maxLat
    )
}

fun MapState.refocus(
    scope: CoroutineScope,
    markers: List<MapPinData>,
    forceZoom: Boolean = true,
    ignoreAccuracy: Boolean = false,
    padding: PaddingValues = PaddingValues(60.dp),
    durationMs: Int = 500
) {
    scope.launch {
        if (markers.isEmpty()) return@launch
        val box = boundsForMarkers(markers = markers, ignoreAccuracy = ignoreAccuracy) ?: return@launch

        // Calculate diagonal distance between bounds corners
        val boundsDistance = MapLibreMapUtils.computeDistanceBetween(
            from = LatLng(lat = box.northeast.latitude, lng = box.northeast.longitude),
            to = LatLng(lat = box.southwest.latitude, lng = box.southwest.longitude)
        )

        val boundsCenter = Position(
            longitude = (box.southwest.longitude + box.northeast.longitude) / 2,
            latitude = (box.southwest.latitude + box.northeast.latitude) / 2
        )

        val animation = CameraAnimation.Fly {
            duration = durationMs.coerceAtLeast(1).milliseconds
        }
        val boundsFit = CameraFit(fitPadding = padding.toDpPadding(LayoutDirection.Ltr))

        val currentVisibleBounds = this@refocus.viewport?.visibleBounds?.toBoundingBox()
        if (currentVisibleBounds != null) {
            val fitBounds = currentVisibleBounds.canFitBounds(box)
            if (forceZoom) {
                // When forceZoom is true, zoom to bounds OR zoom to center if bounds are small
                if (fitBounds && boundsDistance < 500) {
                    // Bounds are small and fit - zoom to center with high zoom level
                    this@refocus.animateCamera(
                        update = cameraPositionCenteredInPadding(
                            center = boundsCenter,
                            zoom = 16.0,
                            padding = padding,
                            bearing = this@refocus.cameraPosition.bearing,
                            pitch = this@refocus.cameraPosition.pitch,
                        ).toCameraUpdate(),
                        animation = animation
                    )
                } else {
                    // Zoom to fit bounds with padding
                    this@refocus.animateCameraToBounds(
                        boundingBox = box,
                        fit = boundsFit,
                        animation = animation
                    )
                }
            } else {
                // When forceZoom is false, only adjust position if needed
                if (fitBounds) {
                    // Bounds fit in current view - just move to center without changing zoom
                    this@refocus.animateCamera(
                        update = cameraPositionCenteredInPadding(
                            center = boundsCenter,
                            zoom = this@refocus.cameraPosition.zoom,
                            padding = padding,
                            bearing = this@refocus.cameraPosition.bearing,
                            pitch = this@refocus.cameraPosition.pitch,
                        ).toCameraUpdate(),
                        animation = animation
                    )
                } else {
                    // Bounds don't fit - need to zoom out to show all
                    this@refocus.animateCameraToBounds(
                        boundingBox = box,
                        fit = boundsFit,
                        animation = animation
                    )
                }
            }
        } else {
            // No viewport yet (map not initialized) - default behavior
            if (boundsDistance < 500) {
                this@refocus.animateCamera(
                    update = CameraPosition(
                        center = boundsCenter,
                        zoom = 16.0,
                        bearing = 0.0,
                        pitch = 0.0,
                    ).toCameraUpdate(),
                    animation = animation
                )
            } else {
                this@refocus.animateCameraToBounds(
                    boundingBox = box,
                    fit = boundsFit,
                    animation = animation
                )
            }
        }
    }
}

fun MapState.focusOnMap(
    scope: CoroutineScope,
    focusType: MapHomeScreenFocusType?,
    markers: List<MapPinData>,
    focusedMarker: FocusedMarkerData?,
    durationMs: Int,
    mapContentPadding: PaddingValues
) {
    val focus = when (focusType) {
        is MapHomeScreenFocusType.PLACE,
        is MapHomeScreenFocusType.PRIVACY_ZONE,
        is MapHomeScreenFocusType.MEMBER -> {
            listOfNotNull(
                markers.firstOrNull { it.markerId == focusedMarker?.markerId }
            )
        }

        MapHomeScreenFocusType.ALL_PLACES -> {
            markers.filter { it.pinType == MapPinType.PLACE }
        }

        MapHomeScreenFocusType.ALL_PRIVACY_ZONES -> {
            markers.filter { it.pinType == MapPinType.PRIVACY_ZONE }
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

        MapHomeScreenFocusType.ALL_PRIVACY_ZONES -> {
            if (markers.size == 1) 40 else 60
        }

        is MapHomeScreenFocusType.LOCATION_HISTORY -> 200

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

/**
 * [CameraPosition] padding stays on the camera and would apply to later moves too.
 * So we are shifting center position so the point sits at the center of usable map, not the full map.
 */
private fun MapState.cameraPositionCenteredInPadding(
    center: Position,
    zoom: Double,
    padding: PaddingValues,
    bearing: Double,
    pitch: Double,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): CameraPosition {
    val metersPerDpAtCurrentZoom = viewport?.metersPerDpAtCenter?.takeIf { it > 0.0 }
        ?: return CameraPosition(center = center, zoom = zoom, bearing = bearing, pitch = pitch)

    val metersPerDpAtDestination = metersPerDpAtCurrentZoom * 2.0.pow(cameraPosition.zoom - zoom)
    val offsetXDp = padding.calculateLeftPadding(layoutDirection)
        .minus(padding.calculateRightPadding(layoutDirection))
        .value / 2f
    val offsetYDp = padding.calculateTopPadding()
        .minus(padding.calculateBottomPadding())
        .value / 2f

    val origin = LatLng(lat = center.latitude, lng = center.longitude)
    val shiftedEastWest = MapLibreMapUtils.computeOffset(
        from = origin,
        distance = abs(offsetXDp) * metersPerDpAtDestination,
        heading = if (offsetXDp >= 0f) 270.0 else 90.0,
    )
    val shifted = MapLibreMapUtils.computeOffset(
        from = shiftedEastWest,
        distance = abs(offsetYDp) * metersPerDpAtDestination,
        heading = if (offsetYDp >= 0f) 0.0 else 180.0,
    )

    return CameraPosition(
        center = Position(longitude = shifted.lng, latitude = shifted.lat),
        zoom = zoom,
        bearing = bearing,
        pitch = pitch,
    )
}

/**
 * Checks if this bounding box can fit the given bounds without needing to zoom out.
 * Compares the width and height of both boxes using spherical distance calculations.
 */
private fun BoundingBox.canFitBounds(bounds: BoundingBox): Boolean {
    // Calculate width (east-west distance) of both boxes
    val thisWidth = MapLibreMapUtils.computeDistanceBetween(
        from = LatLng(this.southwest.latitude, this.southwest.longitude),
        to = LatLng(this.southwest.latitude, this.northeast.longitude)
    )
    val boundsWidth = MapLibreMapUtils.computeDistanceBetween(
        from = LatLng(bounds.southwest.latitude, bounds.southwest.longitude),
        to = LatLng(bounds.southwest.latitude, bounds.northeast.longitude)
    )

    // Calculate height (north-south distance) of both boxes
    val thisHeight = MapLibreMapUtils.computeDistanceBetween(
        from = LatLng(this.southwest.latitude, this.southwest.longitude),
        to = LatLng(this.northeast.latitude, this.southwest.longitude)
    )
    val boundsHeight = MapLibreMapUtils.computeDistanceBetween(
        from = LatLng(bounds.southwest.latitude, bounds.southwest.longitude),
        to = LatLng(bounds.northeast.latitude, bounds.southwest.longitude)
    )

    return thisWidth >= boundsWidth && thisHeight >= boundsHeight
}

internal object MapLibreMapUtils {
    private const val EARTH_RADIUS = 6371009.0

    /**
     * Computes the offset point from a given origin point at a specified distance and heading.
     * Uses the Haversine formula for spherical earth calculations.
     */
    fun computeOffset(
        from: LatLng,
        distance: Double,
        heading: Double
    ): LatLng {
        var distance = distance
        var heading = heading
        distance /= EARTH_RADIUS
        heading = Math.toRadians(heading)
        // http://williams.best.vwh.net/avform.htm#LL
        val fromLat = Math.toRadians(from.lat)
        val fromLng = Math.toRadians(from.lng)
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

    /**
     * Computes the distance between two points on the Earth's surface using the Haversine formula.
     * Returns distance in meters.
     */
    fun computeDistanceBetween(from: LatLng, to: LatLng): Double {
        val fromLat = Math.toRadians(from.lat)
        val fromLng = Math.toRadians(from.lng)
        val toLat = Math.toRadians(to.lat)
        val toLng = Math.toRadians(to.lng)

        val dLat = toLat - fromLat
        val dLng = toLng - fromLng

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(fromLat) * cos(toLat) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }
}