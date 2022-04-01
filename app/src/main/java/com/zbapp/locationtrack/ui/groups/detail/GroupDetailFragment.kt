package com.zbapp.locationtrack.ui.groups.detail

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentGroupDetailBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
import com.zbapp.locationtrack.ui.groups.SharingStartFragmentArgs
import com.zbapp.locationtrack.ui.groups.create.CreateGroupFragmentArgs
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMembersFragmentArgs
import com.zbapp.locationtrack.ui.invite.AccessCodeInputFragmentArgs
import com.zbapp.locationtrack.ui.shared.DeeplinkManager
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.utils.extensions.attributedBold
import com.zbapp.locationtrack.utils.extensions.backgroundTintRes
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class GroupDetailFragment : BaseFragment(R.layout.fragment_group_detail) {

    private val args by navArgs<GroupDetailFragmentArgs>()
    private val vm: GroupDetailViewModel by viewModel { parametersOf(args.groupId) }
    private val vb by viewBinding(FragmentGroupDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.groupDetailsLiveData.observe {
            when (it) {
                Payload.Loading -> {}
                is Payload.Success -> {
                    vb.tvGroupName.text = it.data.name
                    vb.tvGroupEmoji.text = it.data.image
                    vb.bMembers.text =
                        getString(R.string.group_details_section_members_title, it.data.memberCount).attributedBold(requireContext())

                    vb.toolbar.menu.clear()
                    if(it.data.isMyGroup) {
                        vb.toolbar.inflateMenu(R.menu.group_detail_menu_owner)
                    } else {
                        vb.toolbar.inflateMenu(R.menu.group_detail_menu_member)
                    }

                    if(it.data.sharingActive) {
                        vb.bShareStart.backgroundTintRes(R.color.highlightRed)
                        vb.bShareStart.setText(R.string.location_banner_info_stop)
                    } else {
                        vb.bShareStart.backgroundTintRes(R.color.fab_background_color)
                        vb.bShareStart.setText(R.string.groups_sharing_title)
                    }
                }
                is Payload.Error -> {
                    snackBarError(vb.toolbar, it.exception)
                }
            }
        }

        vb.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when (it.itemId) {
                R.id.itemGroupEdit -> {
                    navigateDefault(
                        R.id.createGroupFragment,
                        CreateGroupFragmentArgs(args.groupId).toBundle()
                    )
                    true
                }
                R.id.itemMemberAdd -> {
                    navigateDefault(
                        R.id.acceptInviteFragment,
                        AccessCodeInputFragmentArgs(
                            code = null,
                            groupId = args.groupId
                        ).toBundle()
                    )
                    true
                }
                R.id.itemGroupDelete -> {
                    askToDeleteGroup()
                    true
                }
                R.id.itemGroupLeave -> {
                    askToLeaveGroup()
                    true
                }
                else -> false
            }
        }

        vb.bMembers.setOnClickListener {
            navigateDefault(
                R.id.groupMembersFragment,
                GroupMembersFragmentArgs(
                    groupId = args.groupId,
                    groupName = vm.groupDetails.value.data?.name ?: ""
                ).toBundle()
            )
        }

        vb.mapHolder.setOnClickListener {
            mainActivity?.navigateDeeplink(DeeplinkManager.deeplinkMap(args.groupId))
        }

        vb.bShareStart.setOnClickListener {
            if(vm.isSharingActive()) {
                vm.stopSharing()
            } else {
                navigateDefault(
                    R.id.sharingStartFragment,
                    SharingStartFragmentArgs(args.groupId).toBundle()
                )
            }
        }
    }

    private fun askToDeleteGroup() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alert_title_confirm)
            .setMessage(R.string.group_details_delete_group_alert_title)
            .setPositiveButton(R.string.group_details_delete_group_alert_button) { _, _ ->
                deleteGroup()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun askToLeaveGroup() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alert_title_confirm)
            .setMessage(R.string.group_details_leave_group_alert_title)
            .setPositiveButton(R.string.group_details_leave_group_alert_button) { _, _ ->
                deleteGroup()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun deleteGroup() {
        vm.leaveOrDeleteGroup().observe(viewLifecycleOwner) {
            when (it) {
                Payload.Loading -> {}
                is Payload.Success -> {
                    findNavController().popBackStack()
                }
                is Payload.Error -> {
                    snackBarError(vb.toolbar, it.exception)
                }
            }
        }
    }

}