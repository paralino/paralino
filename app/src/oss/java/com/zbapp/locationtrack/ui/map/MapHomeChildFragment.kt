package com.zbapp.locationtrack.ui.map

import android.os.Bundle
import android.view.View
import androidx.core.util.lruCache
import androidx.preference.PreferenceManager
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.LayoutMapOsmdroidBinding
import com.zbapp.locationtrack.utils.extensions.isDarkThemeOn
import com.zbapp.locationtrack.utils.extensions.viewBinding
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
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow

class MapHomeChildFragment: IMapHomeFragment(R.layout.layout_map_osmdroid) {

    private val vb by viewBinding(LayoutMapOsmdroidBinding::bind)

    private val markerCache = lruCache<String, Marker>(100)

    override var onMapTouch: (() -> Boolean)? = null
    override var onMarkerClick: ((String) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            AppClass.context, PreferenceManager.getDefaultSharedPreferences(AppClass.context)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMapView()
    }

    private fun initMapView() {
        vb.mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        vb.mapView.setMultiTouchControls(true)
        vb.mapView.isTilesScaledToDpi = false
        vb.mapView.isVerticalMapRepetitionEnabled = false
        vb.mapView.minZoomLevel = 4.0
        vb.mapView.controller.setZoom(6.0)
        vb.mapView.setScrollableAreaLimitLatitude(
            MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0
        )
        vb.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        if (context?.isDarkThemeOn() == true)
            vb.mapView.overlayManager?.tilesOverlay?.setColorFilter(TilesOverlay.INVERT_COLORS)

        vb.mapView.tileProvider.clearTileCache()

        vb.mapView.setOnTouchListener { _, _ ->
            onMapTouch?.invoke() ?: false
        }

        vb.mapView.overlays.add(CopyrightOverlay(AppClass.context).apply { setOffset(0, resources.getDimensionPixelSize(
            R.dimen.bottomBar_spacing_edge
        )) })
        vb.mapView.overlays.add(RotationGestureOverlay(vb.mapView))
    }

    override fun updateMarkers(list: List<MemberModel>) {
        synchronized(markerCache) {
            // remove markers not in current list
            markerCache.snapshot().keys.subtract(list.map { MapPinData.fromMember(it).toString() }).forEach {
                markerCache.remove(it)
            }

            list
                .filter { it.mutableData != null }
                .forEach { model ->
                    val pinData = MapPinData.fromMember(model)
                    val marker = markerCache.get(pinData.toString())
                    if (marker != null) {
                        marker.position = GeoPoint(model.mutableData!!.lat, model.mutableData!!.lng)
                        if (marker.relatedObject != pinData) {
                            val pin = getPin(vb.root.context, model)
                            marker.relatedObject = pin.first
                            marker.icon = pin.second
                        }
                    } else {
                        addNewMarker(model, model.mutableData!!)
                    }
                }
        }
    }

    override fun reDrawMarkers() {
        val markers = markerCache.snapshot().values

        val markerToTrack: Marker? = markers.firstOrNull { it.id == trackMarkerId }

        vb.mapView.overlays.removeAll { it is Marker }
        vb.mapView.overlays.addAll(markers)

        if (markers.isNotEmpty()) {
            if (trackPositions) {
                vb.mapView.zoomToBoundingBox(
                    BoundingBox.fromGeoPointsSafe(markers.map { it.position }),
                    true, 100, 18.0, 600
                )
            } else if (markerToTrack != null) {
                vb.mapView.controller.animateTo(
                    markerToTrack.position, vb.mapView.zoomLevelDouble, 600
                )
            } else {
                vb.mapView.postInvalidate()
            }
        } else {
            vb.mapView.postInvalidate()
        }
    }


    private fun addNewMarker(model: MemberModel, data: LocationDataModel) {
        val newMarker = createMarker(model, model.mutableData!!)
        markerCache.put(MapPinData.fromMember(model).toString(), newMarker)
    }

    private fun createMarker(model: MemberModel, data: LocationDataModel): Marker {
        val marker = Marker(vb.mapView)
        val pin = getPin(vb.root.context, model)

        marker.id = model.id
        marker.relatedObject = pin.first
        marker.position = GeoPoint(data.lat, data.lng)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = model.name
        marker.infoWindow = getInfoWindow()
        marker.icon = pin.second
        marker.setOnMarkerClickListener(::onMarkerClick)
        return marker
    }

    private fun onMarkerClick(marker: Marker, mapView: MapView?): Boolean {
        return onMarkerClick?.invoke(marker.id) ?: false
    }

    override fun focusToMarker(markerId: String) {
        markerCache.snapshot().get(markerId)?.let {
            vb.mapView.controller.animateTo(it.position, vb.mapView.zoomLevelDouble, 300)
        }
    }

    private fun getInfoWindow(): InfoWindow {
        return BasicInfoWindow(R.layout.element_map_pin_info, vb.mapView)
    }


    override fun onDestroy() {
        markerCache.evictAll()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        vb.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        vb.mapView.onPause()
    }
}