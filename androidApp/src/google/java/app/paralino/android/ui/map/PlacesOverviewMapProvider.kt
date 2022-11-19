package app.paralino.android.ui.map

import android.util.LruCache
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import app.paralino.android.R
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.list.PlaceListItemData
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.scalebar.scalebar

@Composable
fun rememberMapViewPlacesOverviewState(
    darkMode: Boolean,
    mapView: MapView = rememberMapViewWithLifecycle(darkMode = darkMode),
): MapViewPlacesOverviewProviderState {
    val markerCache: LruCache<String, MarkerCacheData> = remember { lruCache(20) }
    return remember {
        MapViewPlacesOverviewProviderState(
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

class MapViewPlacesOverviewProviderState constructor(
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

    fun updateMarkers(groupId: String, data: List<PlaceListItemData>) {
        val pins = data
            .filter { it.lat != null && it.lng != null }
            .map { MapPinData(groupId, it) }
        setAndUpdateMarkers(pins)

        if(pins.isNotEmpty())
            recenterMap()
    }

    private fun recenterMap() {
        synchronized(markerCache) {
            val markers = markerCache.snapshot().values
            mapView.getMapboxMap().apply {
                val camera = cameraForCoordinates(markers.flatMap { it.polygon?.points?.flatten() ?: emptyList() }).let {
                    it.toBuilder().zoom(it.zoom?.times(0.9)).build()
                }
                cameraAnimationsPlugin { flyTo(camera) }
            }
        }
    }

    override fun focusToMarker(data: MarkerCacheData) {
        // ignoring focusing on one marker in this screen
        recenterMap()
    }

}