package com.zbapp.locationtrack.ui.groups.detail

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentGroupDetailBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
import com.zbapp.locationtrack.ui.groups.detail.members.GroupMembersFragmentArgs
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class GroupDetailFragment : BaseFragment(R.layout.fragment_group_detail) {

    private val args by navArgs<GroupDetailFragmentArgs>()
    private val vm: GroupDetailViewModel by viewModel { parametersOf(args.groupId) }
    private val vb by viewBinding(FragmentGroupDetailBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.groupDetails.observe {
            when (it) {
                Payload.Loading -> {}
                is Payload.Success -> {
                    vb.tvGroupName.text = it.data.name
                    vb.tvGroupEmoji.text = it.data.image
                    vb.bMembers.text =
                        getString(R.string.group_details_section_members_title, it.data.memberCount)
                }
                is Payload.Error -> {
                    snackBarError(vb.toolbar, requireContext())
                }
            }
        }

        vb.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when(it.itemId) {
                R.id.itemGroupEdit -> {
                    true
                }
                R.id.itemGroupDelete -> {
                    askToDeleteGroup()
                    true
                }
                else -> false
            }
        }

        vb.bMembers.setOnClickListener {
            navigateDefault(
                R.id.groupMembersFragment,
                GroupMembersFragmentArgs(
                    args.groupId,
                    vm.groupDetails.value?.data?.name ?: ""
                ).toBundle()
            )
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

    private fun deleteGroup() {
        vm.deleteGroup().observe(viewLifecycleOwner) {
            when (it) {
                Payload.Loading -> {}
                is Payload.Success -> { findNavController().popBackStack() }
                is Payload.Error -> {
                    snackBarError(vb.toolbar, requireContext())
                }
            }
        }
    }

}