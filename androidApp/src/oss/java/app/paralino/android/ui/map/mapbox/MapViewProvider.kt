package app.paralino.android.ui.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.util.LruCache
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.paralino.android.R
import app.paralino.core.BuildKonfig
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.ui.theme.AppColors
import com.google.gson.JsonPrimitive
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptionsManager
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.OnPolygonAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.addOnScaleListener
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.toCameraOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfTransformation
import kotlin.math.pow


@Composable
internal fun rememberMapViewWithLifecycle(
    darkMode: Boolean,
    satellite: Boolean = false
): MapView {
    val context = LocalContext.current
    val mapView = remember(context, darkMode, satellite) {
        val option = ResourceOptionsManager.getDefault(
            context,
            BuildKonfig.mapboxAccessToken
        ).resourceOptions
        MapView(
            context,
            MapInitOptions(
                context,
                resourceOptions = option,
                styleUri = if (satellite) {
                    Style.SATELLITE_STREETS
                } else if (darkMode) {
                    Style.DARK
                } else {
                    Style.MAPBOX_STREETS
                }
            )
        )
    }

    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = remember(mapView, satellite) {
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

data class MarkerCacheData(
    val data: MapPinData,
    val point: PointAnnotation,
    val polygon: PolygonAnnotation?,
)

open class MapViewProviderState constructor(
    protected val mapView: MapView,
    protected val markerCache: LruCache<String, MarkerCacheData>,
    protected val annotationsManager: PointAnnotationManager?,
    protected val polygonAnnotationsManager: PolygonAnnotationManager?,
) : IMapViewProviderState {

    override val mapAndroidView: MapView
        get() = mapView

    val isSatellite: Boolean
        get() = mapView.getMapboxMap().getStyle()?.styleURI == Style.SATELLITE_STREETS

    override var onMapTouch: (() -> Boolean)? = null
    override var onMapTouchEnd: (() -> Unit)? = null
    override var onMarkerClick: ((MapPinData) -> Boolean)? = null

    override fun initMap() {
        mapView.apply {
            // disable telemetry by default
            attribution.getMapAttributionDelegate().telemetry()
                .disableTelemetrySession()
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
        }

        initListeners()
    }

    protected fun initListeners() {
        mapView.apply {
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
                        override fun onMoveEnd(detector: MoveGestureDetector) {
                            onMapTouchEnd?.invoke()
                        }

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
                markerCache.snapshot()?.values?.firstOrNull { it.point.id == annotation.id }?.let {
                    if (!annotation.isSelected) {
                        focusToMarker(it)
                    }
                    onMarkerClick?.invoke(it.data)
                    annotation.isSelected = true
                }
                return@OnPointAnnotationClickListener true
            })

            polygonAnnotationsManager?.addClickListener(OnPolygonAnnotationClickListener { polygon ->
                markerCache.snapshot()?.values?.firstOrNull { it.point.id == polygon.id }?.let {
                    if (!polygon.isSelected) {
                        focusToMarker(it)
                    }
                    onMarkerClick?.invoke(it.data)
                }
                return@OnPolygonAnnotationClickListener true
            })
        }
    }

    protected fun updatePolygonsForMapSize() {
        val currentCamera = mapView.getMapboxMap().cameraState.toCameraOptions()
        synchronized(markerCache) {
            markerCache.snapshot().forEach {
                it.value?.let { markerData ->
                    markerData.polygon?.let {
                        val camera = mapView.getMapboxMap().cameraForGeometry(it.geometry)
                        val diff = (currentCamera.zoom ?: 0.0) - (camera.zoom ?: 0.0)
                        // change polygon opacity based on size on screen
                        val diffFactor = when (markerData.data.pinType) {
                            MapPinType.MEMBER -> if (isSatellite) 0.3 else 0.2
                            MapPinType.PLACE -> if (isSatellite) 0.4 else 0.2
                        }
                        val staticFactor = when (markerData.data.pinType) {
                            MapPinType.MEMBER -> if (isSatellite) 0.15 else 0.2
                            MapPinType.PLACE -> if (isSatellite) 0.2 else 0.1
                        }
                        val isZoomedOut = diff < 0
                        val factor = if (isZoomedOut) {
                            0.0
                        } else {
                            diff.pow(2).coerceAtMost(6.0).div(6)
                        }
                        it.fillOpacity = (1 - factor) * diffFactor + staticFactor
                        polygonAnnotationsManager?.update(it)
                    }
                }
            }
        }
    }

    override fun reDrawMarkers(focusedMarkerData: FocusedMarkerData?, focusType: MapHomeScreenFocusType) {
        val markers = markerCache.snapshot().values

        val markerToTrack = markers.firstOrNull { it.data.markerId == focusedMarkerData?.markerId }

        if (markerToTrack != null) {
            focusToMarker(markerToTrack)
        } else if (focusType == MapHomeScreenFocusType.ALL_PLACES || focusType == MapHomeScreenFocusType.ALL_MEMBERS) {

            val markersToTrack = when (focusType) {
                MapHomeScreenFocusType.ALL_PLACES -> {
                    markers.filter { it.data.pinType == MapPinType.PLACE }
                }
                MapHomeScreenFocusType.ALL_MEMBERS -> {
                    markers.filter { it.data.pinType == MapPinType.MEMBER }
                }
                else -> emptyList()
            }

            if(markersToTrack.isNotEmpty()) {
                mapView.getMapboxMap().apply {
                    // TODO review if fixed: https://github.com/mapbox/mapbox-maps-android/issues/611
                    // Currently we do this workaround to focus on bounds (bounding box in osmdroid)
                    // and it will jump without animation if zoom > maxZoom
                    setBounds(CameraBoundsOptions.Builder().maxZoom(15.0).build())
                    val camera = cameraForCoordinates(markersToTrack.map { it.point.point }).let {
                        it.toBuilder().zoom(it.zoom?.times(0.9)).build()
                    }
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
            }
        }
    }

    fun setAndUpdateMarkers(list: List<MapPinData>) {
        synchronized(markerCache) {
            // remove markers not in current list
            markerCache.snapshot().minus(list.map { it.toString() })
                .forEach {
                    markerCache.remove(it.key)
                    annotationsManager?.delete(it.value.point)
                    it.value.polygon?.let { it1 -> polygonAnnotationsManager?.delete(it1) }
                }

            list.forEach { pinData ->
                val marker = markerCache.get(pinData.toString())
                if (marker != null) {
                    // update marker
                    updateMarker(pinData, marker)
                } else {
                    // create new marker
                    addNewMarker(pinData)
                }
            }
        }
    }

    protected fun addNewMarker(model: MapPinData) {
        val pin = MapUserMarkerView.create(mapView.context, model)

        val newPoint = PointAnnotationOptions()
            .withData(JsonPrimitive(model.markerId))
            .withPoint(
                Point.fromLngLat(
                    model.lng,
                    model.lat
                )
            )
            .withIconImage(pin.second.bitmap)
            .withIconAnchor(
                when (model.markerStyle) {
                    MapMarkerStyle.PIN -> IconAnchor.BOTTOM
                    MapMarkerStyle.CIRCLE -> IconAnchor.CENTER
                }
            )
            .let {
                annotationsManager?.create(it)!!
            }

        val newPolygon = if (model.shouldShowApproximateCircle) {
            PolygonAnnotationOptions()
                .withGeometry(
                    TurfTransformation.circle(
                        Point.fromLngLat(
                            model.lng,
                            model.lat
                        ),
                        model.accuracy.toDouble(),
                        360,
                        TurfConstants.UNIT_METERS
                    )
                )
                .let {
                    when (model.pinType) {
                        MapPinType.MEMBER -> {
                            it.withFillColor(Color.BLUE)
                                .withFillOpacity(if (isSatellite) 0.45 else 0.4)
                        }

                        MapPinType.PLACE -> {
                            it.withFillColor(AppColors.accent.toArgb())
                                .withFillOpacity(if (isSatellite) 0.45 else 0.3)
                        }
                    }
                }
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

    protected fun updateMarker(newMarkerData: MapPinData, marker: MarkerCacheData) {
        Point.fromLngLat(newMarkerData.lng, newMarkerData.lat).let {
            marker.point.point = it
            marker.polygon?.geometry = TurfTransformation.circle(
                it,
                newMarkerData.accuracy.toDouble(),
                360,
                TurfConstants.UNIT_METERS
            )
        }

        if (marker.data != newMarkerData) {
            // marker content has changed
            val pin = MapUserMarkerView.create(mapView.context, newMarkerData)
            marker.point.iconImageBitmap = pin.second.bitmap
            val newMarker = marker.copy(data = pin.first)
            markerCache.put(marker.data.markerId, newMarker)
        }

        annotationsManager?.update(marker.point)
        marker.polygon?.let { polygonAnnotationsManager?.update(it) }
    }

    private fun resetMapMaxZoom() {
        mapView.getMapboxMap().setBounds(CameraBoundsOptions.Builder().maxZoom(22.0).build())
    }

    override fun focusToMarker(markerId: String) {
        markerCache.snapshot().values.firstOrNull { it.data.markerId == markerId }?.let {
            focusToMarker(it)
        }
    }

    protected open fun focusToMarker(data: MarkerCacheData) {

        val maxZoom = when (data.data.pinType) {
            MapPinType.MEMBER -> 16.0
            MapPinType.PLACE -> 16.0
        }

        val currentZoom = mapView.getMapboxMap().cameraState.toCameraOptions().zoom ?: maxZoom

        val cameraPosition = data.polygon?.let {
            val camera = mapView.getMapboxMap().cameraForGeometry(it.geometry)
            val targetZoom = camera.zoom?.coerceAtMost(maxZoom)?.times(0.98f)
            val zoom = if (currentZoom < (targetZoom ?: 0.0)) {
                targetZoom
            } else if(currentZoom > (camera.zoom ?: 0.0)) {
                camera.zoom?.times(0.98f)
            } else {
                currentZoom
            }
            camera.toBuilder()
                .zoom(zoom)
                .build()
        } ?: CameraOptions.Builder()
            .zoom(if (currentZoom < maxZoom) maxZoom else currentZoom)
            .center(data.point.point)
            .build()

        // set camera position
        mapView.getMapboxMap().cameraAnimationsPlugin { this.flyTo(cameraPosition) }
    }

    fun isMarkerFocused(markerId: String): Boolean {
        return markerCache.snapshot().values.firstOrNull { it.data.markerId == markerId }?.point?.isSelected
            ?: false
    }
}