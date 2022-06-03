package com.zbapp.locationtrack.ui.map

import android.annotation.SuppressLint
import android.util.LruCache
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.util.lruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.zbapp.core.presentation.map.MapMarkerState
import com.zbapp.locationtrack.utils.extensions.isDarkThemeOn
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


@Composable
fun rememberMapViewProviderState(
    darkMode: Boolean,
    mapView: MapView = rememberMapViewWithLifecycle(darkMode = darkMode),
): IMapViewProviderState {
    val markerCache: LruCache<String, Marker> = remember { lruCache(100) }
    return remember {
        MapViewProviderState(
            mapView = mapView,
            markerCache = markerCache
        )
    }
}

@Composable
private fun rememberMapViewWithLifecycle(darkMode: Boolean): MapView {
    val context = LocalContext.current
    val mapView = remember(context, darkMode) {
        Configuration.getInstance().load(
            context, PreferenceManager.getDefaultSharedPreferences(context)
        )
        MapView(context)
    }

    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

private class MapViewProviderState(
    private val mapView: MapView,
    private val markerCache: LruCache<String, Marker>,
) : IMapViewProviderState {

    override val mapAndroidView: View
        get() = mapView

    override var onMapTouch: (() -> Boolean)? = null
    override var onMarkerClick: ((String) -> Boolean)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun initMap() {
        mapView.apply {
            setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            setMultiTouchControls(true)
            isTilesScaledToDpi = false
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 4.0
            controller.setZoom(6.0)
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0
            )
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            if (context?.isDarkThemeOn() == true)
                overlayManager?.tilesOverlay?.setColorFilter(TilesOverlay.INVERT_COLORS)

            tileProvider.clearTileCache()

            this.setOnTouchListener { _, _ ->
                onMapTouch?.invoke()
                false
            }

            overlays.add(CopyrightOverlay(context))
            overlays.add(RotationGestureOverlay(mapView))
        }
    }

    override fun updateMarkers(list: List<MapMarkerState>) {
        synchronized(markerCache) {
            // remove markers not in current list
            markerCache.snapshot().keys.subtract(list.map { MapPinData(it).toString() }).forEach {
                markerCache.remove(it)
            }

            list
                .forEach { model ->
                    val pinData = MapPinData(model)
                    val marker = markerCache.get(pinData.toString())
                    if (marker != null) {
                        marker.position = GeoPoint(model.locationData.lat, model.locationData.lng)
                        if (marker.relatedObject != pinData) {
                            val pin = MapUserMarkerView.create(mapView.context, model)
                            marker.relatedObject = pin.first
                            marker.icon = pin.second
                        }
                    } else {
                        addNewMarker(model)
                    }
                }
        }
    }

    override fun reDrawMarkers(trackMarkerId: String?, trackPositions: Boolean) {
        val markers = markerCache.snapshot().values

        val markerToTrack: Marker? = markers.firstOrNull { it.id == trackMarkerId }

        mapView.overlays.removeAll { it is Marker }
        mapView.overlays.addAll(markers)

        if (markers.isNotEmpty()) {
            if (trackPositions) {
                mapView.zoomToBoundingBox(
                    BoundingBox.fromGeoPointsSafe(markers.map { it.position }),
                    true, 100, 18.0, 600
                )
            } else if (markerToTrack != null) {
                mapView.controller.animateTo(
                    markerToTrack.position, mapView.zoomLevelDouble, 600
                )
            } else {
                mapView.postInvalidate()
            }
        } else {
            mapView.postInvalidate()
        }
    }

    private fun addNewMarker(model: MapMarkerState) {
        val newMarker = createMarker(model)
        markerCache.put(MapPinData(model).toString(), newMarker)
    }

    private fun createMarker(model: MapMarkerState): Marker {
        val marker = Marker(mapView)
        val pin = MapUserMarkerView.create(mapView.context, model)

        marker.id = model.memberData.id
        marker.relatedObject = pin.first
        marker.position = GeoPoint(model.locationData.lat, model.locationData.lng)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = model.memberData.name
        marker.infoWindow = null
        marker.icon = pin.second
        marker.setOnMarkerClickListener { marker, mapView ->
            onMarkerClick?.invoke(marker.id) ?: false
        }
        return marker
    }

    override fun focusToMarker(markerId: String) {
        markerCache.snapshot().get(markerId)?.let {
            mapView.controller.animateTo(it.position, mapView.zoomLevelDouble, 300)
        }
    }
}