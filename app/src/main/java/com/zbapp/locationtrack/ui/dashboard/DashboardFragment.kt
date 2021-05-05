package com.zbapp.locationtrack.ui.dashboard

import android.os.Bundle
import android.view.View
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentDashboardBinding
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.tracking.LocationService
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
import com.zbapp.locationtrack.ui.shared.DeeplinkManager
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetActionData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetItems
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetTitleData
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpace
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptyState
import com.zbapp.locationtrack.ui.shared.list.ListSubtitleItem
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.simpleAdapter
import com.zbapp.locationtrack.utils.withClickAction
import com.zbapp.locationtrack.utils.withDataOnClickAction
import org.koin.androidx.viewmodel.ext.android.viewModel

class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {

    private val vm: DashboardViewModel by viewModel()
    private val binding by viewBinding(FragmentDashboardBinding::bind)
    private lateinit var permissionHandler: LocationPermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUi()
        setupObservers()
    }

    private fun setUi() {
        val adapter = SimpleRvAdapter()
            .addFactory {
                ErrorListItem(it).withDataOnClickAction {
                    when(it) {
                        ErrorListItemType.GPS -> showNoLocationServicesWarning()
                        ErrorListItemType.PERMISSION -> showNoLocationPermissionWarning()
                    }
                }
            }
            .addFactory {
                GroupListItem(it).withClickAction {
                    vm.deleteGroup(it.itemData?.group?.id ?: "")
                }.apply {
                    onGroupActionClick = { vm.stopTracking() }
                }
            }
            .addFactory { ListSubtitleItem(it) }
            .addFactory { ListItemEmptySpace(it) }
            .addFactory {
                GroupAddItem(it).withClickAction {
                    navigateDefault(R.id.createGroupFragment)
                }
            }
            .addFactory {
                ListItemEmptyState(it).withClickAction {
                    mainActivity?.navigateDeeplink(DeeplinkManager.deeplinkLogin)
                }
            }

        binding.rvDashboard.adapter = adapter
    }

    private fun setupObservers() {

        binding.bShare.setOnClickListener {
            if(vm.isTrackingActive()) {
                vm.stopTracking()
            } else {
                permissionHandler.updateRequestStatus()
            }
        }

        vm.getGroups().observe {
            binding.rvDashboard.simpleAdapter?.submitList(it)
        }

        permissionHandler.status.observe {
            vm.locationStatus.value = it
        }

        permissionHandler.requestStatus.observe {
            when {
                !it.isPermissionAllowed -> {
                    showNoLocationPermissionWarning()
                }
                !it.isGpsOn -> {
                    showNoLocationServicesWarning()
                }
                it.isOK() -> {
                    vm.startTracking()
                    LocationService.start(context ?: return@observe)
                }
            }
        }

    }

    private fun showNoLocationPermissionWarning() {
        BottomSheetDialog.show(
            this, mutableListOf(
                BottomSheetTitleData("Location permission"),
                BottomSheetActionData("ok", R.string.button_ok),
                BottomSheetItems.CANCEL
            )
        ) { _, bundle ->
            if (BottomSheetDialog.getAction(bundle)?.id == "ok") {
                permissionHandler.askLocationPermission(requireActivity())
            }
        }
    }


    private fun showNoLocationServicesWarning() {
        BottomSheetDialog.show(
            this, mutableListOf(
                BottomSheetTitleData("Location gps"),
                BottomSheetActionData("ok", R.string.button_ok),
                BottomSheetItems.CANCEL
            )
        ) { _, bundle ->
            if (BottomSheetDialog.getAction(bundle)?.id == "ok") {
                permissionHandler.askLocationService()
            }
        }
    }

}