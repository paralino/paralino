package com.zbapp.locationtrack.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.zbapp.core.models.Payload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentMapBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.dashboard.GroupItemData
import com.zbapp.locationtrack.ui.groups.GroupPickerFragment
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMemberItemData
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMemberListItem
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.safeCast
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.simpleAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel


class MapHomeRootFragment : BaseFragment(R.layout.fragment_map) {

    private val args by lazy { arguments?.let { MapHomeRootFragmentArgs.fromBundle(it) } ?: MapHomeRootFragmentArgs() }
    private val vm: MapViewModel by viewModel()
    private val vb by viewBinding(FragmentMapBinding::bind)
    val fragment get() = vb.fragmentMap.getFragment<IMapHomeFragment>()

    /**
     * focus and track all markers
     */
    private var trackPositions: Boolean
        get() = fragment.trackPositions
        set(value) {
            fragment.trackPositions = value
            vb.fabGroup.isSelected = value
            if(value) {
                trackMarkerId = null
            }
        }

    /**
     * id of specific marker to track and focus on
     */
    private var trackMarkerId: String?
        get() = fragment.trackMarkerId
        set(value) {
            fragment.trackMarkerId = value
            vb.rvUsers.isVisible = !value.isNullOrBlank()
            if(!value.isNullOrBlank()) {
                trackPositions = false
            }
        }

    private var trackingGroupId: String? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.switchGroup(args.groupId ?: "")

        vb.fabGroup.isSelected = trackPositions
        vb.bGroupSelector.clipToOutline = true

        vb.fabGroup.setOnClickListener {
            trackPositions = true
            fragment.reDrawMarkers()
        }

        vb.rvUsers.adapter = SimpleRvAdapter()
            .addFactory {
                GroupMemberListItem(forViewPager = true, it).also {
                    it.onMemberDelete = {
                        it.toggleState()
                        vm.clearMyLocation().observe {
                            when(it) {
                                Payload.Loading -> {}
                                is Payload.Error -> snackBarError(vb.fabGroup, it.exception)
                                is Payload.Success -> {}
                            }
                        }
                    }
                }
            }

        vb.rvUsers.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                vb.rvUsers.simpleAdapter?.currentList?.getOrNull(position)?.safeCast<GroupMemberItemData>()?.let { member ->
                    selectMarker(member.member.id)
                }
            }
        })

        fragment.onMarkerClick = { onMarkerClick(it) }
        fragment.onMapTouch = {
            trackPositions = false
            trackMarkerId = null
            false
        }

        vm.getMarkers().observe {

            vb.bGroupSelector.isVisible = it is Payload.Success
            vb.fabGroup.isVisible = it is Payload.Success

            when(it) {
                Payload.Loading -> { }
                is Payload.Success -> {

                    if(trackingGroupId != it.data.group.id) {
                        trackPositions = true
                    }
                    trackingGroupId = it.data.group.id

                    vb.tvGroupEmoji.text = it.data.group.image.getImageValue()
                    vb.tvGroupName.text = it.data.group.name

                    val inactiveMembers = it.data.group.members.count { it.mutableData == null }
                    if(inactiveMembers == 0) {
                        // not using plural form for 0 since 0 is treated as "other" in some languages
                        vb.tvGroupCount.text = "${it.data.group.memberCount}"
                    } else {
                        vb.tvGroupCount.text = resources.getQuantityString(
                            R.plurals.group_sharing_member_active_info,
                            inactiveMembers,
                            it.data.group.memberCount,
                            inactiveMembers
                        )
                    }

                    updateMarkers(it.data)
                }
                is Payload.Error -> {
                    snackBarError(vb.fabGroup, it.exception)
                }
            }
            fragment.reDrawMarkers()
        }

        vm.getSharingTime().observe {
            vb.tvShareTime.isVisible = !it.isNullOrBlank()
            vb.tvShareTime.text = it ?: ""
        }

        vm.locationRefreshSubscription.observe {
            // to keep location refresh alive while ui is alive
        }

        vb.bGroupSelector.setOnClickListener {
            pickGroup()
        }
    }

    private fun pickGroup() {
        vm.getAllGroups().observe { groups ->
            BottomSheetDialog.show(
                this,
                GroupPickerFragment(),
                groups.map { GroupItemData(it, GroupItemData.GroupUiType.PICKER) }.toMutableList()
            ) { _, bundle ->
                BottomSheetDialog.getAction(bundle)?.id?.let {
                    vm.switchGroup(it)
                }
            }
        }
    }

    private fun updateMarkers(data: MapUiModel) {
        fragment.updateMarkers(data.group.members)
        vb.rvUsers.simpleAdapter?.submitList(data.memberListData)
    }

    private fun onMarkerClick(markerId: String): Boolean {
        vb.rvUsers.simpleAdapter?.currentList?.indexOfFirst {
            (it as? GroupMemberItemData)?.member?.id == markerId
        }?.takeIf { it >= 0 }?.let {
            vb.rvUsers.currentItem = it
        }
        selectMarker(markerId)
        return false
    }

    private fun selectMarker(markerId: String) {
        trackMarkerId = markerId
        fragment.focusToMarker(markerId)
    }

}
