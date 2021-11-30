package com.zbapp.locationtrack.ui.invite

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zbapp.core.models.Payload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentInviteAcceptBinding
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetTitleData
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class AccessCodeInputFragment : BottomSheetDialogFragment() {

    private val vb by viewBinding(FragmentInviteAcceptBinding::bind)
    private val vm by viewModel<AcceptInviteViewModel>()
    private val args by lazy { arguments?.let { AccessCodeInputFragmentArgs.fromBundle(it) } }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogStyle
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_invite_accept, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUI()
        setupObservers()
    }

    private fun setUI() {
        vb.contentHolder.layoutTransition = LayoutTransition().apply {
            setAnimateParentHierarchy(false)
        }
        vb.title.itemData =
            BottomSheetTitleData(requireContext().getString(R.string.invite_connect_title))
        vb.etInvite.editText?.doAfterTextChanged {
            vm.invalidateCode()
        }

        vm.acceptOrLoadInvite(args?.code)
    }

    private fun setupObservers() {

        vb.btConfirm.setOnClickListener {
            vm.acceptOrLoadInvite(vb.etInvite.editText?.text?.toString() ?: "")
        }

        vm.inviteCodeLiveData.observe(viewLifecycleOwner) {
            vb.btConfirm.setLoading(it is Payload.Loading, withCheck = false)
            vb.etInvite.isEnabled = it !is Payload.Loading
            when (it) {
                Payload.Loading -> {
                }
                is Payload.Success -> {
                    val data = it.data
                    vb.tvInviteName.isVisible = data is AcceptInviteEvent.InviteLoaded
                    vb.tvInviteImage.isVisible = data is AcceptInviteEvent.InviteLoaded
                    vb.tvInviteDescription.isVisible = data is AcceptInviteEvent.InviteLoaded && !data.data.description.isNullOrBlank()
                    vb.tvGroupCount.isVisible =
                        data is AcceptInviteEvent.InviteLoaded && data.data.groupCount != null

                    vb.btConfirm.setContent(data.data.actionButtonText)
                    vb.tvGroupCount.text = data.data.groupCount?.toString() ?: "0"
                    vb.tvInviteName.text = data.data.name
                    vb.tvInviteImage.text = data.data.imageEmoji
                    vb.tvInviteDescription.text = data.data.description
                }
                is Payload.Error -> {
                    snackBarError(vb.btConfirm, requireContext())
                }
            }
        }
    }

}