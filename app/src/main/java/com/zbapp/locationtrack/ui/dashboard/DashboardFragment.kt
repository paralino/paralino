package com.zbapp.locationtrack.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentDashboardBinding
import com.zbapp.locationtrack.tracking.LocationConditionStatus
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.LocationService
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetActionData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetItems
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetTitleData
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {

    private val vm: DashboardViewModel by viewModel()
    val binding by viewBinding(FragmentDashboardBinding::bind)
    private lateinit var permissionHandler: LocationPermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bannerTracking.root.setOnClickListener {
            permissionHandler.updateStatus()
        }

        vm.locationInit.currentLocation.asLiveData().observe {
            Toast.makeText(context, "got location ${it != null}", Toast.LENGTH_SHORT).show()
            binding.bannerTracking.tvLocationStatusSubtitle.isVisible = it != null
            if(it != null) {
                binding.bannerTracking.tvLocationStatusTitle.setText(R.string.location_banner_tracking_active)
            } else {
                binding.bannerTracking.tvLocationStatusTitle.setText(R.string.location_banner_tracking_start)
            }
        }

        permissionHandler.status.observe {
            when(it) {
                LocationConditionStatus.LOCATION_PERMISSION_DENIED -> {
                    showNoLocationPermissionWarning()
                }
                LocationConditionStatus.LOCATION_SERVICE_DISABLED -> {
                    showNoLocationServicesWarning()
                }
                LocationConditionStatus.OK -> {
                    LocationService.start(context ?: return@observe)
                }
            }
        }
    }


    private fun showNoLocationPermissionWarning() {
        BottomSheetDialog.show(
            childFragmentManager, mutableListOf(
                BottomSheetTitleData("Location permission"),
                BottomSheetActionData("ok", R.string.button_ok),
                BottomSheetItems.CANCEL
            )
        ) {
            if (it.id == "ok") {
                permissionHandler.askLocationPermission(requireActivity())
            }
        }
    }


    private fun showNoLocationServicesWarning() {
        BottomSheetDialog.show(
            childFragmentManager, mutableListOf(
                BottomSheetTitleData("Location gps"),
                BottomSheetActionData("ok", R.string.button_ok),
                BottomSheetItems.CANCEL
            )
        ) {
            if (it.id == "ok") {
                permissionHandler.askLocationService()
            }
        }
    }

}