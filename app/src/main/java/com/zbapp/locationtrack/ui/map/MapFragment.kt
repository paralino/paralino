package com.zbapp.locationtrack.ui.map

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceManager
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentMapBinding
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapFragment : BaseFragment(R.layout.fragment_map) {

    private lateinit var vm: MapViewModel
    private lateinit var permissionHandler: LocationPermissionHandler
    private val vb by viewBinding(FragmentMapBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
        Configuration.getInstance().load(
            AppClass.context, PreferenceManager.getDefaultSharedPreferences(
                AppClass.context
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        vb.mapView.tileProvider.clearTileCache()

        vb.mapView.overlays.add(
            MyLocationNewOverlay(GpsMyLocationProvider(context), vb.mapView).apply {
                enableFollowLocation()
                enableMyLocation()
            }
        )

        vb.mapView.overlays.add(CopyrightOverlay(AppClass.context))
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