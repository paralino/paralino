package com.zbapp.locationtrack.ui.invite

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.zbapp.core.models.Payload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentInviteManageBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.dashboard.GroupAddItem
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.utils.SimpleRvAdapter
import com.zbapp.locationtrack.utils.extensions.copy
import com.zbapp.locationtrack.utils.extensions.share
import com.zbapp.locationtrack.utils.extensions.viewBinding
import com.zbapp.locationtrack.utils.simpleAdapter
import com.zbapp.locationtrack.utils.withClickAction
import org.koin.androidx.viewmodel.ext.android.viewModel

class ManageInviteFragment : BaseFragment(R.layout.fragment_invite_manage) {

    val vm: ManageInviteViewModel by viewModel()
    val vb: FragmentInviteManageBinding by viewBinding(FragmentInviteManageBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        vb.rvInvites.adapter = SimpleRvAdapter()
            .addFactory {
                InviteCodeListItem(it).also {
                    it.onCopy = {
                        context?.copy(it.invite.inviteCode)
                        context?.let {
                            Toast.makeText(
                                it,
                                it.getString(R.string.toast_copy_invite_code),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    it.onShare = {
                        context?.share(it.invite.inviteUrl)
                    }
                    it.onDelete = {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Are you sure you want to delete invite code?")
                            .setNegativeButton(R.string.button_ok) { _, _ ->

                            }
                            .setPositiveButton(R.string.button_cancel) { _, _ ->
                                deleteInvite(it)
                            }
                            .show()
                    }
                }
            }
            .addFactory {
                GroupAddItem(it).withClickAction {
                    addInvite()
                }
            }
    }

    private fun setupObservers() {
        vm.getAllInvites().observe {
            when (it) {
                Payload.Loading -> {
                    // show loading
                }
                is Payload.Success -> {
                    vb.rvInvites.simpleAdapter?.submitList(it.data)
                }
                is Payload.Error -> {
                    snackBarError(vb.rvInvites, requireContext())
                }
            }
        }
    }

    private fun addInvite() {
        vm.createInviteCode(1).observe {
            when (it) {
                Payload.Loading -> {
                    // TODO add loading
                }
                is Payload.Error -> {
                    snackBarError(vb.rvInvites, requireContext())
                }
                is Payload.Success -> {
                    // nothing?
                }
            }
        }
    }

    private fun deleteInvite(code: InviteCodeData) {
        vm.deleteInvite(code.invite).observe {
            when (it) {
                Payload.Loading -> {
                    // TODO add loading
                }
                is Payload.Error -> {
                    snackBarError(vb.rvInvites, requireContext())
                }
                is Payload.Success -> {
                    // nothing?
                }
            }
        }
    }

}