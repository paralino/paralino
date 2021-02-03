package com.zbapp.locationtrack.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationPermissionHandler

class MapFragment : Fragment(R.layout.fragment_map) {

    private lateinit var homeViewModel: MapViewModel
    private lateinit var permissionHandler: LocationPermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}