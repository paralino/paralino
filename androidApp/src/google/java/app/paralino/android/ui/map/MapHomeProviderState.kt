package app.paralino.android.ui.map

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.util.lruCache
import app.paralino.core.presentation.map.MapPinData
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager

@Composable
fun rememberMapViewHomeState(
    darkMode: Boolean,
    satellite: Boolean,
    mapView: MapView = rememberMapViewWithLifecycle(darkMode = darkMode, satellite = satellite),
): MapViewHomeProviderState {
    val markerCache: LruCache<String, MarkerCacheData> = remember { lruCache(100) }
    return remember {
        MapViewHomeProviderState(
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

class MapViewHomeProviderState constructor(
    mapView: MapView,
    markerCache: LruCache<String, MarkerCacheData>,
    annotationsManager: PointAnnotationManager?,
    polygonAnnotationsManager: PolygonAnnotationManager?,
) : MapViewProviderState(mapView, markerCache, annotationsManager, polygonAnnotationsManager) {

    fun updateMarkers(list: List<MapPinData>) {
        setAndUpdateMarkers(list)
    }

}
