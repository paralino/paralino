package app.paralino.android.ui.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.util.LruCache
import android.view.Gravity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.paralino.android.R
import app.paralino.core.presentation.map.MapMarkerState
import com.google.gson.JsonPrimitive
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.*
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import kotlin.math.abs

@Composable
fun rememberMapViewProviderState(
    darkMode: Boolean,
    mapView: MapView = rememberMapViewWithLifecycle(darkMode = darkMode),
): IMapViewProviderState {
    val markerCache: LruCache<String, MarkerCacheData> = remember { lruCache(100) }
    return remember {
        MapViewProviderState(
            mapView = mapView,
            markerCache = markerCache,
            annotationsManager = mapView.annotations.createPointAnnotationManager(AnnotationConfig(
                layerId = "markers")),
            polygonAnnotationsManager = mapView.annotations.createPolygonAnnotationManager(
                AnnotationConfig(belowLayerId = "markers")),
        )
    }
}

@Composable
private fun rememberMapViewWithLifecycle(darkMode: Boolean): MapView {
    val context = LocalContext.current
    val mapView = remember(context, darkMode) {
        val option = ResourceOptionsManager.getDefault(
            context,
            app.paralino.android.BuildConfig.mapboxAccessToken
        ).resourceOptions
        MapView(
            context,
            MapInitOptions(
                context,
                resourceOptions = option,
                styleUri = if (darkMode) Style.DARK else Style.MAPBOX_STREETS
            )
        )
    }

    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
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

private data class MarkerCacheData(
    val data: MapPinData,
    val point: PointAnnotation,
    val polygon: PolygonAnnotation?,
)

private class MapViewProviderState constructor(
    private val mapView: MapView,
    private val markerCache: LruCache<String, MarkerCacheData>,
    private val annotationsManager: PointAnnotationManager?,
    private val polygonAnnotationsManager: PolygonAnnotationManager?,
) : IMapViewProviderState {

    override val mapAndroidView: View
        get() = mapView

    override var onMapTouch: (() -> Boolean)? = null
    override var onMarkerClick: ((String) -> Boolean)? = null

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
                this.marginTop =
                    context.resources.getDimensionPixelSize(R.dimen.map_north_margin).toFloat()
            }

            scalebar.updateSettings { this.enabled = false }

            getMapboxMap().apply {
                addOnMapClickListener {
                    annotationsManager?.annotations?.forEach { it.isSelected = false }
                    polygonAnnotationsManager?.annotations?.forEach { it.isSelected = false }
                    onMapTouch?.invoke()
                    false
                }

                addOnScaleListener(object : OnScaleListener {
                    override fun onScale(detector: StandardScaleGestureDetector) {
                        updatePolygonsForMapSize()
                    }

                    override fun onScaleBegin(detector: StandardScaleGestureDetector) {}
                    override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
                })

                addOnMoveListener(
                    object : OnMoveListener {
                        override fun onMove(detector: MoveGestureDetector) = false
                        override fun onMoveEnd(detector: MoveGestureDetector) {}

                        override fun onMoveBegin(detector: MoveGestureDetector) {
                            updatePolygonsForMapSize()
                            annotationsManager?.annotations?.forEach { it.isSelected = false }
                            polygonAnnotationsManager?.annotations?.forEach {
                                it.isSelected = false
                            }
                            onMapTouch?.invoke()
                        }
                    }
                )
            }

            annotationsManager?.addClickListener(OnPointAnnotationClickListener { annotation ->
                if (!annotation.isSelected) {
                    focusToMarker(annotation)
                }
                markerCache.snapshot()?.values?.firstOrNull { it.point.id == annotation.id }?.let {
                    onMarkerClick?.invoke(it.data.memberId)
                }
                return@OnPointAnnotationClickListener true
            })

            polygonAnnotationsManager?.addClickListener(OnPolygonAnnotationClickListener { polygon ->
                markerCache.snapshot()?.values?.firstOrNull { it.point.id == polygon.id }?.let {
                    if (!polygon.isSelected) {
                        focusToMarker(it.point)
                    }
                    onMarkerClick?.invoke(it.data.memberId)
                }
                return@OnPolygonAnnotationClickListener true
            })

        }
    }

    private fun updatePolygonsForMapSize() {
        val currentCamera = mapView.getMapboxMap().cameraState.toCameraOptions()
        synchronized(markerCache) {
            markerCache.snapshot().forEach {
                it.value?.let {
                    it.polygon?.let {
                        val camera = mapView.getMapboxMap().cameraForGeometry(it.geometry)
                        val show = abs((camera.zoom ?: 0.0) - (currentCamera.zoom ?: 0.0))
                        // change polygon opacity based on size on screen
                        it.fillOpacity = if (show > 4) 0.0 else 0.2
                        polygonAnnotationsManager?.update(it)
                    }
                }
            }
        }
    }

    override fun updateMarkers(list: List<MapMarkerState>) {
        synchronized(markerCache) {
            // remove markers not in current list
            markerCache.snapshot().minus(list.map { MapPinData(it).toString() })
                .forEach {
                    markerCache.remove(it.key)
                    annotationsManager?.delete(it.value.point)
                    it.value.polygon?.let { it1 -> polygonAnnotationsManager?.delete(it1) }
                }

            list
                .forEach { model ->
                    val pinData = MapPinData(model)
                    val marker = markerCache.get(pinData.toString())
                    if (marker != null) {
                        // update marker
                        Point.fromLngLat(model.locationData.lng, model.locationData.lat).let {
                            marker.point.point = it
                            marker.polygon?.geometry = TurfTransformation.circle(
                                it,
                                model.locationData.accuracy.toDouble(),
                                360,
                                TurfConstants.UNIT_METERS
                            )
                        }
                        if (marker.data != pinData) {
                            // marker content has changed
                            val pin = MapUserMarkerView.create(mapView.context, model)
                            marker.point.iconImageBitmap = pin.second.bitmap
                            val newMarker = marker.copy(data = pin.first)
                            markerCache.put(marker.data.memberId, newMarker)
                        }
                        annotationsManager?.update(marker.point)
                        marker.polygon?.let { polygonAnnotationsManager?.update(it) }
                    } else {
                        // create new marker
                        val pin = MapUserMarkerView.create(mapView.context, model)

                        val newPoint = PointAnnotationOptions()
                            .withData(JsonPrimitive(model.memberData.id))
                            .withPoint(
                                Point.fromLngLat(
                                    model.locationData.lng,
                                    model.locationData.lat
                                )
                            )
                            .withIconImage(pin.second.bitmap)
                            .withIconAnchor(IconAnchor.BOTTOM)
                            .let {
                                annotationsManager?.create(it)!!
                            }

                        val newPolygon = if (model.locationData.shouldShowApproximateCircle) {
                            PolygonAnnotationOptions()
                                .withGeometry(
                                    TurfTransformation.circle(
                                        Point.fromLngLat(
                                            model.locationData.lng,
                                            model.locationData.lat
                                        ),
                                        model.locationData.accuracy.toDouble(),
                                        360,
                                        TurfConstants.UNIT_METERS
                                    )
                                )
                                .withFillColor(Color.BLUE)
                                .withFillOpacity(0.2)
                                .let {
                                    polygonAnnotationsManager?.create(it)
                                }
                        } else {
                            null
                        }

                        markerCache.put(
                            pin.first.toString(),
                            MarkerCacheData(
                                data = pin.first,
                                point = newPoint,
                                polygon = newPolygon
                            )
                        )
                    }
                }
        }
    }

    override fun reDrawMarkers(trackMarkerId: String?, trackPositions: Boolean) {
        val markers = markerCache.snapshot().values
        val markerToTrack = markers.firstOrNull { it.data.memberId == trackMarkerId }

        if (markers.isNotEmpty()) {
            if (trackPositions) {
                mapView.getMapboxMap().apply {
                    // TODO review if fixed: https://github.com/mapbox/mapbox-maps-android/issues/611
                    // Currently we do this workaround to focus on bounds (bounding box in osmdroid)
                    // and it will jump without animation if zoom > maxZoom
                    setBounds(CameraBoundsOptions.Builder().maxZoom(15.0).build())
                    val camera = cameraForCoordinates(markers.map { it.point.point })
                    cameraAnimationsPlugin {
                        flyTo(camera, MapAnimationOptions.mapAnimationOptions {
                            animatorListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    resetMapMaxZoom()
                                }

                                override fun onAnimationCancel(animation: Animator) {
                                    resetMapMaxZoom()
                                }
                            })
                        })
                    }
                }
            } else if (markerToTrack != null) {
                focusToMarker(markerToTrack.point)
            }
        }
    }

    private fun resetMapMaxZoom() {
        mapView.getMapboxMap().setBounds(CameraBoundsOptions.Builder().maxZoom(22.0).build())
    }

    override fun focusToMarker(markerId: String) {
        markerCache.snapshot().values.firstOrNull { it.data.memberId == markerId }?.let {
            focusToMarker(it.point)
        }
    }

    private fun focusToMarker(annotation: PointAnnotation) {
        val cameraPosition = CameraOptions.Builder()
            .zoom(16.0)
            .center(annotation.point)
            .build()
        // set camera position
        mapView.getMapboxMap().cameraAnimationsPlugin { this.flyTo(cameraPosition) }
    }
}