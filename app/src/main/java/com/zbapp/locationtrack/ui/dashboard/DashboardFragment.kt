package com.zbapp.locationtrack.ui.dashboard

import android.os.Bundle
import android.view.View
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.LocationService
import com.zbapp.locationtrack.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_dashboard.*

class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {

    private lateinit var vm: DashboardViewModel
    private lateinit var permissionHandler: LocationPermissionHandler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        text_dashboard.setOnClickListener {
            val text = text_dashboard.text.toString()
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