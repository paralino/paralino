package com.zbapp.locationtrack.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.LocationService
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var permissionHandler: LocationPermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionHandler.status.observe(viewLifecycleOwner) {
            text_home.text = it.name
        }

        text_home.setOnClickListener {
            val text = text_home.text.toString()
            if(text == LocationConditionStatus.LOCATION_PERMISSION_DENIED.name) {
                permissionHandler.askLocationPermission(null)
            } else if(text == LocationConditionStatus.LOCATION_SERVICE_DISABLED.name) {
                permissionHandler.askLocationService()
            } else {
                LocationService.start(context ?: return@setOnClickListener)
            }
        }

    }
}