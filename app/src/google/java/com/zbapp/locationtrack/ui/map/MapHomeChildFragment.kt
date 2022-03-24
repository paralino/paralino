package com.zbapp.locationtrack.ui.map

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.lruCache
import com.google.gson.JsonPrimitive
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
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

class MapHomeChildFragment: IMapHomeFragment() {

//    private val vb by viewBinding(LayoutMapMapboxBinding::bind)

    private val markerCache = lruCache<String, PointAnnotation>(100)

    private var annotationsManager: PointAnnotationManager? = null
    private var mapView: MapView? = null
        set(value) {
            field = value
            if(value != null) {
                annotationsManager = value.annotations.createPointAnnotationManager().apply {
                    addClickListener(
                        OnPointAnnotationClickListener {
                            // define camera position
                            if(!it.isSelected) {
                                val cameraPosition = CameraOptions.Builder()
                                    .zoom(15.0)
                                    .center(it.point)
                                    .build()
                                // set camera position
                                mapView?.getMapboxMap()
                                    ?.cameraAnimationsPlugin { this.flyTo(cameraPosition) }
                            }
                            false
                            onMarkerClick?.invoke(it.getData()?.asJsonPrimitive?.asString ?: "")
                            true
                        }
                    )
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
        val pins = list.filter { it.mutableData != null }.map {
            PointAnnotationOptions()
                .withData(JsonPrimitive(it.id))
                .withPoint(Point.fromLngLat(it.mutableData!!.lng, it.mutableData!!.lat))
                .withIconImage(getPin(requireContext(), it).second.bitmap)
                .withIconAnchor(IconAnchor.BOTTOM)
        }
        annotationsManager?.create(pins)
        annotationsManager?.annotations
    }

    override fun reDrawMarkers() {

    }

    override fun focusToMarker(markerId: String) {
//        markerCache.snapshot().get(markerId)?.let {
//
//        }
    }

    override fun onDestroyView() {
        mapView = null
        super.onDestroyView()
    }
}