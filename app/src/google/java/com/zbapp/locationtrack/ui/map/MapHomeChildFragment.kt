package com.zbapp.locationtrack.ui.map

import android.animation.Animator
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import com.google.gson.JsonPrimitive
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.ClusterOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.zbapp.core.models.location.MemberModel
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.utils.extensions.isDarkThemeOn

class MapHomeChildFragment: IMapHomeFragment(), OnPointAnnotationClickListener {

//    private val vb by viewBinding(LayoutMapMapboxBinding::bind)

    private val markerCache = lruCache<String, MarkerData>(100)

    private var annotationsManager: PointAnnotationManager? = null
    private var mapView: MapView? = null
        set(value) {
            field = value
            if(value != null) {
                annotationsManager = value.annotations.createPointAnnotationManager().apply {
                    addClickListener(this@MapHomeChildFragment)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val option = ResourceOptionsManager.getDefault(requireContext(), BuildConfig.mapboxAccessToken)
        return MapView(
            requireContext(),
            MapInitOptions(
                requireContext(),
                resourceOptions = option.resourceOptions,
                styleUri = if(AppClass.context.isDarkThemeOn()) Style.DARK else Style.MAPBOX_STREETS
            )
        ).also {
            mapView = it
            val margin = requireContext().resources.getDimensionPixelSize(R.dimen.bottomBar_spacing_edge).toFloat()
            it.logo.updateSettings { marginBottom = margin }
            it.attribution.updateSettings { marginBottom = margin }
            it.attribution.getMapAttributionDelegate().telemetry().setUserTelemetryRequestState(enabled = false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView?.compass?.updateSettings {
            this.enabled = true
            this.position = Gravity.TOP or Gravity.END
            this.image = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_north_indicator)
            this.marginRight = requireContext().resources.getDimensionPixelSize(R.dimen.activity_side_margin_small).toFloat()
            this.marginTop = requireContext().resources.getDimensionPixelSize(R.dimen.map_north_margin).toFloat()
        }
        mapView?.scalebar?.updateSettings { this.enabled = false }

        mapView?.getMapboxMap()?.addOnMapClickListener {
            annotationsManager?.annotations?.forEach { it.isSelected = false }
            onMapTouch?.invoke()
            false
        }

        mapView?.getMapboxMap()?.addOnMoveListener(object: OnMoveListener {
            override fun onMove(detector: MoveGestureDetector): Boolean { return false }
            override fun onMoveEnd(detector: MoveGestureDetector) { }

            override fun onMoveBegin(detector: MoveGestureDetector) {
                annotationsManager?.annotations?.forEach { it.isSelected = false }
                onMapTouch?.invoke()
            }
        })
    }

    override fun updateMarkers(list: List<MemberModel>) {
        synchronized(markerCache) {
            // remove markers not in current list
            markerCache.snapshot().minus(list.map { MapPinData.fromMember(it).toString() }).forEach {
                markerCache.remove(it.key)
                annotationsManager?.delete(it.value.point)
            }

            list
                .filter { it.mutableData != null }
                .forEach { model ->
                    val pinData = MapPinData.fromMember(model)
                    val marker = markerCache.get(pinData.toString())
                    if (marker != null) {
                        marker.point.point = Point.fromLngLat(model.mutableData!!.lng, model.mutableData!!.lat)
                        if (marker.data != pinData) {
                            val pin = getPin(requireContext(), model)
                            marker.point.iconImageBitmap = pin.second.bitmap
                            val newMarker = marker.copy(data = pin.first)
                            markerCache.put(marker.data.memberId, newMarker)
                        }
                        annotationsManager?.update(marker.point)
                    } else {
                        val pin = getPin(requireContext(), model)
                        val new = PointAnnotationOptions()
                            .withData(JsonPrimitive(model.id))
                            .withPoint(Point.fromLngLat(model.mutableData!!.lng, model.mutableData!!.lat))
                            .withIconImage(pin.second.bitmap)
                            .withIconAnchor(IconAnchor.BOTTOM)
                        val point = annotationsManager?.create(new)!!
                        markerCache.put(pin.first.toString(), MarkerData(pin.first, point))
                    }
                }
        }

    }

    override fun reDrawMarkers() {
        val markers = markerCache.snapshot().values
        val markerToTrack = markers.firstOrNull { it.data.memberId == trackMarkerId }

        if (markers.isNotEmpty()) {
            if (trackPositions) {
                mapView?.getMapboxMap()?.apply {
                    setBounds(CameraBoundsOptions.Builder().maxZoom(15.0).build())
                    val camera = cameraForCoordinates(markers.map { it.point.point })
                    cameraAnimationsPlugin {
                        flyTo(camera, MapAnimationOptions.mapAnimationOptions {
                            animatorListener(object: Animator.AnimatorListener {
                                override fun onAnimationStart(animation: Animator?) {}
                                override fun onAnimationRepeat(animation: Animator?) {}

                                override fun onAnimationEnd(animation: Animator?) {
                                    resetMapMaxZoom()
                                }

                                override fun onAnimationCancel(animation: Animator?) {
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

    fun resetMapMaxZoom() {
        mapView?.getMapboxMap()?.setBounds(CameraBoundsOptions.Builder().maxZoom(22.0).build())
    }

    override fun focusToMarker(markerId: String) {
        markerCache.snapshot().values.firstOrNull { it.data.memberId == markerId }?.let {
            focusToMarker(it.point)
        }
    }

    fun focusToMarker(annotation: PointAnnotation) {
        val cameraPosition = CameraOptions.Builder()
            .zoom(16.0)
            .center(annotation.point)
            .build()
        // set camera position
        mapView?.getMapboxMap()
            ?.cameraAnimationsPlugin { this.flyTo(cameraPosition) }
    }

    override fun onDestroyView() {
        mapView = null
        super.onDestroyView()
    }

    override fun onAnnotationClick(annotation: PointAnnotation): Boolean {
        // define camera position
        if(!annotation.isSelected) {
            focusToMarker(annotation)
        }
        markerCache.snapshot()?.values?.firstOrNull { it.point.id == annotation.id }?.let {
            onMarkerClick?.invoke(it.data.memberId)
        }
        return true
    }
}

data class MarkerData(
    val data: MapPinData,
    val point: PointAnnotation
)