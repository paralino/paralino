package com.zbapp.locationtrack.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceManager
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


class MapFragment : BaseFragment(R.layout.fragment_map) {

    private val vm: MapViewModel by viewModel()
    private lateinit var permissionHandler: LocationPermissionHandler
    private val vb by viewBinding(FragmentMapBinding::bind)

    private var trackPositions: Boolean = true
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
            var markerToTrack: Marker? = null
            val markers = it.map { model ->
                val startMarker = Marker(vb.mapView)
                startMarker.id = model.userId
                startMarker.position = GeoPoint(model.lat, model.lng)
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                startMarker.infoWindow = null
                startMarker.setOnMarkerClickListener(::onMarkerClick)

                if(model.userId == trackMarkerId)
                    markerToTrack = startMarker

                startMarker
            }

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
                        markerToTrack!!.position, vb.mapView.zoomLevelDouble, 100
                    )
                } else {
                    vb.mapView.postInvalidate()
                }
            }
        }

    }

    fun onMarkerClick(marker: Marker, mapView: MapView?): Boolean {
        trackMarkerId = marker.id
        trackPositions = false
        vb.fabGroup.isSelected = false
        vb.mapView.controller.animateTo(marker.position, vb.mapView.zoomLevelDouble, 100)
        return false
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