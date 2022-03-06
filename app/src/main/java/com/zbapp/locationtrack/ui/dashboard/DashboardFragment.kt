package com.zbapp.locationtrack.ui.dashboard

import android.os.Bundle
import android.view.View
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentDashboardBinding
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
import com.zbapp.locationtrack.ui.groups.detail.GroupDetailFragmentArgs
import com.zbapp.locationtrack.ui.shared.DeeplinkManager
import com.zbapp.locationtrack.ui.shared.list.*
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
                    when (it) {
                        ErrorListItemType.GPS -> permissionHandler.askLocationService()
                        ErrorListItemType.PERMISSION -> permissionHandler.askLocationPermission()
                    }
                }
            }
            .addFactory {
                GroupListItem(it).withClickAction {
                    it.itemData?.id?.let {
                        navigateDefault(
                            R.id.groupDetailFragment,
                            GroupDetailFragmentArgs(it).toBundle()
                        )
                    }
                }.apply {
                    onGroupActionClick = { vm.stopTracking(it.id) }
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
            if (vm.isSharingActive()) {
                vm.stopAllTracking()
            } else {
                navigateDefault(R.id.sharingStartFragment)
            }
        }

        vm.getGroups().observe {
            binding.rvDashboard.simpleAdapter?.submitList(it)
        }

    }

}