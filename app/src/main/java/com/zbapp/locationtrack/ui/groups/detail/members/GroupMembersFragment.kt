package com.zbapp.locationtrack.ui.groups.detail.members

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.MemberModel
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentGroupMembersBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
import com.zbapp.locationtrack.ui.invite.AccessCodeInputFragmentArgs
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptySpace
import com.zbapp.locationtrack.ui.shared.list.ListItemEmptyState
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.simpleAdapter
import com.zbapp.locationtrack.utils.withClickAction
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class GroupMembersFragment : BaseFragment(R.layout.fragment_group_members) {

    private val args by navArgs<GroupMembersFragmentArgs>()
    private val vm: GroupMembersViewModel by viewModel { parametersOf(args.groupId) }
    private val vb by viewBinding(FragmentGroupMembersBinding::bind)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.toolbar.title = args.groupName
        vb.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when (it.itemId) {
                R.id.itemMemberAdd -> {
                    addNewMember()
                    true
                }
                else -> false
            }
        }

        vb.rvMembers.adapter = SimpleRvAdapter()
            .addFactory {
                GroupMemberListItem(it).also {
                    it.onMemberDelete = { askToRemoveMember(it.member) }
                }
            }
            .addFactory { ListItemEmptyState(it).withClickAction { addNewMember() } }
            .addFactory { ListItemEmptySpace(it) }

        vm.groupMembers.observe(viewLifecycleOwner) {
            vb.rvMembers.simpleAdapter?.submitList(it)
        }
    }

    private fun addNewMember() {
        navigateDefault(
            R.id.acceptInviteFragment,
            AccessCodeInputFragmentArgs(
                code = null,
                groupId = args.groupId
            ).toBundle()
        )
    }

    private fun askToRemoveMember(member: MemberModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alert_title_confirm)
            .setMessage(getString(R.string.group_members_remove_alert_title, member.name))
            .setPositiveButton(R.string.group_members_item_remove_button) { _, _ ->
                deleteMember(member)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun deleteMember(member: MemberModel) {
        vm.removeMember(member).observe(viewLifecycleOwner) {
            when (it) {
                Payload.Loading -> {}
                is Payload.Success -> {}
                is Payload.Error -> {
                    snackBarError(vb.rvMembers, it.exception)
                }
            }
        }
    }

}