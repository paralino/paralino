package com.zbapp.locationtrack.ui.map

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.MemberModel
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
import org.koin.core.parameter.parametersOf


class MapHomeRootFragment : BaseFragment(R.layout.fragment_map) {

    private val args by lazy { arguments?.let { MapHomeRootFragmentArgs.fromBundle(it) } ?: MapHomeRootFragmentArgs() }
    private val vm: MapViewModel by viewModel { parametersOf(args.groupId) }
    private val vb by viewBinding(FragmentMapBinding::bind)
    val fragment get() = vb.fragmentMap.getFragment<IMapHomeFragment>()

    /**
     * focus and track all markers
     */
    private var trackPositions: Boolean
        get() = fragment.trackPositions
        set(value) { fragment.trackPositions = value }

    /**
     * id of specific marker to track and focus on
     */
    private var trackMarkerId: String?
        get() = fragment.trackMarkerId
        set(value) { fragment.trackMarkerId = value }

    private var trackingGroupId: String? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.fabGroup.isSelected = trackPositions
        vb.bGroupSelector.clipToOutline = true

        vb.fabGroup.setOnClickListener {
            trackPositions = true
            trackMarkerId = null
            vb.fabGroup.isSelected = true
            vb.rvUsers.isVisible = false
            fragment.reDrawMarkers()
        }

        vb.rvUsers.adapter = SimpleRvAdapter()
            .addFactory { GroupMemberListItem(forViewPager = true, it) }

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
            vb.fabGroup.isSelected = false
            vb.rvUsers.isVisible = false
            false
        }

        vm.getMarkers().observe {
            when(it) {
                Payload.Loading -> { }
                is Payload.Success -> {

                    if(trackingGroupId != it.data.id) {
                        trackPositions = true
                        vb.fabGroup.isSelected = true
                        vb.rvUsers.isVisible = false
                    }
                    trackingGroupId = it.data.id

                    vb.tvGroupEmoji.text = it.data.emoji
                    vb.tvGroupName.text = it.data.name

                    val inactiveMembers = it.data.members.count { it.mutableData == null }
                    if(inactiveMembers == 0) {
                        // not using plural form for 0 since 0 is treated as "other" in some languages
                        vb.tvGroupCount.text = "$inactiveMembers"
                    } else {
                        vb.tvGroupCount.text = resources.getQuantityString(
                            R.plurals.group_sharing_member_active_info,
                            inactiveMembers,
                            it.data.members.size,
                            inactiveMembers
                        )
                    }

                    updateMarkers(it.data.members)
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

    private fun updateMarkers(list: List<MemberModel>) {
        fragment.updateMarkers(list)
        vb.rvUsers.simpleAdapter?.submitList(
            list.map { GroupMemberItemData(it, false, GroupMemberItemData.MemberUiType.MAP) }
        )
    }

    private fun onMarkerClick(markerId: String): Boolean {
        vb.rvUsers.isVisible = !vb.rvUsers.isVisible
        vb.rvUsers.simpleAdapter?.currentList?.indexOfFirst {
            (it as? GroupMemberItemData)?.member?.id == markerId
        }?.takeIf { it >= 0 }?.let {
            vb.rvUsers.currentItem = it
        }
        selectMarker(markerId)
        return false
    }

    private fun selectMarker(markerId: String) {
        trackPositions = false
        vb.fabGroup.isSelected = false
        trackMarkerId = markerId
        fragment.focusToMarker(markerId)
    }

}
