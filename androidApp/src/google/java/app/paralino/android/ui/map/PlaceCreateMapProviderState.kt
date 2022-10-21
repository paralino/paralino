package app.paralino.android.ui.map

import android.util.LruCache
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import app.paralino.android.R
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation

@Composable
fun rememberMapViewPlaceCreateState(
    darkMode: Boolean,
    mapView: MapView = rememberMapViewWithLifecycle(darkMode = darkMode),
): MapViewPlaceCreateProviderState {
    val markerCache: LruCache<String, MarkerCacheData> = remember { lruCache(20) }
    return remember {
        MapViewPlaceCreateProviderState(
            mapView = mapView,
            markerCache = markerCache,
            annotationsManager = mapView.annotations.createPointAnnotationManager(
                AnnotationConfig(layerId = "markers")
            ),
            polygonAnnotationsManager = mapView.annotations.createPolygonAnnotationManager(
                AnnotationConfig(belowLayerId = "markers")
            ),
        )
    }
}

class MapViewPlaceCreateProviderState constructor(
    mapView: MapView,
    markerCache: LruCache<String, MarkerCacheData>,
    annotationsManager: PointAnnotationManager?,
    polygonAnnotationsManager: PolygonAnnotationManager?,
) : MapViewProviderState(mapView, markerCache, annotationsManager, polygonAnnotationsManager) {

    var onMapLongPress: ((Double, Double) -> Boolean)? = null

    override fun initMap() {
        mapView.apply {
            // disable telemetry by default
            attribution.getMapAttributionDelegate().telemetry()
                .setUserTelemetryRequestState(enabled = false)

            compass.updateSettings {
                this.enabled = true
                this.position = Gravity.TOP or Gravity.END
                this.image = ContextCompat.getDrawable(context, R.drawable.ic_map_north_indicator)
                this.marginRight =
                    context.resources.getDimensionPixelSize(R.dimen.activity_side_margin_small)
                        .toFloat()
            }

            scalebar.updateSettings { this.enabled = false }

            getMapboxMap().apply {
                addOnMapLongClickListener {
                    onMapLongPress?.invoke(it.latitude(), it.longitude()) ?: false
                }
            }
        }
        initListeners()
    }

    fun updateMarkers(data: PlaceDetailScreenState) {
        val pin = data.takeIf { it.lat != null && it.lng != null }?.let {
            listOf(MapPinData(it))
        } ?: emptyList()
        setAndUpdateMarkers(pin)
        if(pin.isNotEmpty())
            recenterMap()
    }

    fun updateMarkers(data: CreatePlaceScreenState) {
        val pin = data.takeIf { it.lat != null && it.lng != null }?.let {
            listOf(MapPinData(it))
        } ?: emptyList()
        setAndUpdateMarkers(pin)
        if(pin.isNotEmpty())
            recenterMap()
    }

    private fun recenterMap() {
        synchronized(markerCache) {
            val point = markerCache.snapshot().entries.firstOrNull()?.value ?: return
            focusToMarker(point.point)
        }
    }

    fun updateRadius(radiusMeters: Int) {
        synchronized(markerCache) {
            val point = markerCache.snapshot().entries.firstOrNull()?.value ?: return

            point.polygon?.geometry = TurfTransformation.circle(
                point.point.point,
                radiusMeters.toDouble(),
                360,
                TurfConstants.UNIT_METERS
            )

            point.polygon?.let { polygon ->
                polygonAnnotationsManager?.update(polygon)
            }

            focusToMarker(point.point)
        }
    }

    override fun focusToMarker(annotation: PointAnnotation) {
        synchronized(markerCache) {
            markerCache.snapshot().entries.firstOrNull()?.value?.polygon?.let {
                val camera = mapView.getMapboxMap().cameraForGeometry(it.geometry).let {
                    it.toBuilder().zoom(it.zoom?.times(0.95)).build()
                }
                mapView.getMapboxMap().cameraAnimationsPlugin { this.flyTo(camera) }
            }
        }
    }

}
