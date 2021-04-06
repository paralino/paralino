package com.zbapp.locationtrack.ui.map

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.util.lruCache
import androidx.preference.PreferenceManager
import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentMapBinding
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.utils.extensions.isDarkThemeOn
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow


class MapFragment : BaseFragment(R.layout.fragment_map) {

    private val vm: MapViewModel by viewModel()
    private lateinit var permissionHandler: LocationPermissionHandler
    private val vb by viewBinding(FragmentMapBinding::bind)

    /**
     * Cache of current markers displayed on map
     */
    private val markerCache = lruCache<String, Marker>(100)

    /**
     * focus and track all markers
     */
    private var trackPositions: Boolean = true

    /**
     * id of specific marker to track and focus on
     */
    private var trackMarkerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
        Configuration.getInstance().load(
            AppClass.context, PreferenceManager.getDefaultSharedPreferences(
                AppClass.context
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.fabGroup.isSelected = trackPositions

        vb.fabGroup.setOnClickListener {
            trackPositions = true
            vb.fabGroup.isSelected = true
        }

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

        if(requireContext().isDarkThemeOn())
            vb.mapView.overlayManager?.tilesOverlay?.setColorFilter(TilesOverlay.INVERT_COLORS)

        vb.mapView.tileProvider.clearTileCache()

        vb.mapView.setOnTouchListener { _, _ ->
            trackPositions = false
            trackMarkerId = null
            vb.fabGroup.isSelected = false
            false
        }

//        vb.mapView.overlays.add(
//            MyLocationNewOverlay(GpsMyLocationProvider(context), vb.mapView).apply {
//                enableFollowLocation()
//                enableMyLocation()
//            }
//        )

        vb.mapView.overlays.add(CopyrightOverlay(AppClass.context))

        vm.getMarkers().observe {
            updateMarkers(it)
            reDrawMarkers()
        }

    }

    private fun updateMarkers(list: List<BaseLocationModel>) {
        synchronized(markerCache) {
            // remove markers not in current list
            markerCache.snapshot().keys.subtract(list.map { it.userId }).forEach {
                markerCache.remove(it)
            }

            list.forEach { model ->
                val marker = markerCache.get(model.userId)
                if(marker != null) {
                    marker.position = GeoPoint(model.lat, model.lng)
                    if(marker.relatedObject?.toString() != model.imageBase64)
                        marker.icon = getPin(model.imageBase64)
                } else {
                    val newMarker = createMarker(model)
                    markerCache.put(model.userId, newMarker)
                }
            }
        }
    }

    private fun reDrawMarkers() {
        val markers = markerCache.snapshot().values

        val markerToTrack: Marker? = markers.firstOrNull { it.id == trackMarkerId }

        vb.mapView.overlays.removeAll { it is Marker }
        vb.mapView.overlays.addAll(markers)

        if(markers.isNotEmpty()) {
            if (trackPositions) {
                vb.mapView.zoomToBoundingBox(
                    BoundingBox.fromGeoPointsSafe(markers.map { it.position }),
                    true, 100, 18.0, 100
                )
            } else if (markerToTrack != null) {
                vb.mapView.controller.animateTo(
                    markerToTrack.position, vb.mapView.zoomLevelDouble, 100
                )
            } else {
                vb.mapView.postInvalidate()
            }
        }
    }

    private fun createMarker(model: BaseLocationModel): Marker {
        val marker = Marker(vb.mapView)
        marker.id = model.userId
        marker.relatedObject = model.imageBase64
        marker.position = GeoPoint(model.lat, model.lng)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = model.name
        marker.infoWindow = getInfoWindow()
        marker.icon = getPin(model.imageBase64)
        marker.setOnMarkerClickListener(::onMarkerClick)
        return marker
    }

    private fun getInfoWindow(): InfoWindow {
        return BasicInfoWindow(R.layout.element_map_pin_info, vb.mapView)
    }

    private fun getPin(imageBase64: String?): BitmapDrawable {
        val bitmap = MapPin.populateWithBase64(vb.mapView, imageBase64)
        return BitmapDrawable(requireContext().resources, bitmap)
    }

    fun onMarkerClick(marker: Marker, mapView: MapView?): Boolean {
        trackMarkerId = marker.id
        trackPositions = false
        vb.fabGroup.isSelected = false
        vb.mapView.controller.animateTo(marker.position, vb.mapView.zoomLevelDouble, 100)
        if(marker.isInfoWindowShown)
            marker.closeInfoWindow()
        else marker.showInfoWindow()
        return false
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