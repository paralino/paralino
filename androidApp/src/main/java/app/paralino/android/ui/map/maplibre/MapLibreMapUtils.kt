package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.models.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.math.sqrt
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
                val center = LatLng(latitude = it.lat, longitude = it.lng)
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
    padding: PaddingValues = PaddingValues(60.dp),
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

        // Calculate diagonal distance between bounds corners
        val boundsDistance = MapLibreMapUtils.computeDistanceBetween(
            from = LatLng(bounds.northEast.latitude, bounds.northEast.longitude),
            to = LatLng(bounds.southWest.latitude, bounds.southWest.longitude)
        )

        val boundsCenter = Position(
            longitude = (bounds.southWest.longitude + bounds.northEast.longitude) / 2,
            latitude = (bounds.southWest.latitude + bounds.northEast.latitude) / 2
        )

        val effectiveDuration = durationMs.coerceAtLeast(1).milliseconds

        val currentVisibleBounds = this@refocus.projection?.queryVisibleBoundingBox()
        if (currentVisibleBounds != null) {
            val fitBounds = currentVisibleBounds.canFitBounds(box)
            if (forceZoom) {
                // When forceZoom is true, zoom to bounds OR zoom to center if bounds are small
                if (fitBounds && boundsDistance < 500) {
                    // Bounds are small and fit - zoom to center with high zoom level
                    this@refocus.animateTo(
                        finalPosition = CameraPosition(
                            target = boundsCenter,
                            zoom = 16.0,
                            bearing = this@refocus.position.bearing,
                            tilt = this@refocus.position.tilt,
                            padding = padding
                        ),
                        duration = effectiveDuration
                    )
                } else {
                    // Zoom to fit bounds with padding
                    this@refocus.animateTo(
                        boundingBox = box,
                        padding = padding,
                        duration = effectiveDuration
                    )
                }
            } else {
                // When forceZoom is false, only adjust position if needed
                if (fitBounds) {
                    // Bounds fit in current view - just move to center without changing zoom
                    this@refocus.animateTo(
                        finalPosition = CameraPosition(
                            target = boundsCenter,
                            zoom = this@refocus.position.zoom,
                            bearing = this@refocus.position.bearing,
                            tilt = this@refocus.position.tilt,
                            padding = padding
                        ),
                        duration = effectiveDuration
                    )
                } else {
                    // Bounds don't fit - need to zoom out to show all
                    this@refocus.animateTo(
                        boundingBox = box,
                        padding = padding,
                        duration = effectiveDuration
                    )
                }
            }
        } else {
            // No projection yet (map not initialized) - default behavior
            if (boundsDistance < 500) {
                this@refocus.animateTo(
                    finalPosition = CameraPosition(
                        target = boundsCenter,
                        zoom = 16.0,
                        bearing = 0.0,
                        tilt = 0.0,
                        padding = padding
                    ),
                    duration = effectiveDuration
                )
            } else {
                this@refocus.animateTo(
                    boundingBox = box,
                    padding = padding,
                    duration = effectiveDuration
                )
            }
        }
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

    /**
     * Computes the distance between two points on the Earth's surface using the Haversine formula.
     * Returns distance in meters.
     */
    fun computeDistanceBetween(from: LatLng, to: LatLng): Double {
        val fromLat = Math.toRadians(from.latitude)
        val fromLng = Math.toRadians(from.longitude)
        val toLat = Math.toRadians(to.latitude)
        val toLng = Math.toRadians(to.longitude)

        val dLat = toLat - fromLat
        val dLng = toLng - fromLng

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(fromLat) * cos(toLat) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }
}