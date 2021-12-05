package com.zbapp.locationtrack.ui.invite

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentInviteAcceptBinding
import com.zbapp.locationtrack.ui.dashboard.GroupItemData
import com.zbapp.locationtrack.ui.groups.GroupPickerFragment
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetTitleData
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.ui.shared.snackBarSimple
import com.zbapp.locationtrack.utils.extensions.dp2px
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AccessCodeInputFragment : BottomSheetDialogFragment() {

    private val vb by viewBinding(FragmentInviteAcceptBinding::bind)
    private val args by lazy { arguments?.let { AccessCodeInputFragmentArgs.fromBundle(it) } }
    private val vm by viewModel<AcceptInviteViewModel> { parametersOf(args) }

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
        vb.etInvite.editText?.setText(args?.code ?: "")
        vb.contentHolder.layoutTransition = LayoutTransition().apply {
            setAnimateParentHierarchy(false)
        }
        vb.title.itemData =
            BottomSheetTitleData(requireContext().getString(R.string.invite_connect_title))
        vb.etInvite.editText?.doAfterTextChanged {
            vm.invalidateCode()
            vb.btConfirm.isEnabled = !it.isNullOrBlank()
        }
    }

    private fun setupObservers() {

        vb.tvGroup.setOnClickListener {
            pickGroup()
        }

        vb.btConfirm.setOnClickListener {
            if (vm.canPickGroup() && !vm.isGroupValid()) {
                setGroupButtonState(error = true)
                snackBarSimple(vb.btConfirm, getString(R.string.invite_connect_alert_pickGroup))
            } else {
                vm.acceptOrLoadInvite(vb.etInvite.editText?.text?.toString() ?: "")
            }
        }

        vm.currentGroupLiveData.observe(viewLifecycleOwner) {
            setGroupButtonState(error = false)
            vb.tvGroup.text = it?.name ?: getString(R.string.invite_connect_button_pickGroup)
        }

        vm.inviteCodeLiveData.observe(viewLifecycleOwner) {
            vb.btConfirm.setLoading(
                loading = it is Payload.Loading,
                withCheck = it.data is AcceptInviteEvent.InviteAccepted
            )
            vb.etInvite.isEnabled = it !is Payload.Loading
            when (it) {
                Payload.Loading -> {}
                is Payload.Success -> {
                    val data = it.data
                    vb.tvInviteName.isVisible = data is AcceptInviteEvent.InviteLoaded
                    vb.tvInviteImage.isVisible = data is AcceptInviteEvent.InviteLoaded
                    vb.tvInviteDescription.isVisible =
                        data is AcceptInviteEvent.InviteLoaded && !data.data.description.isNullOrBlank()
                    vb.tvGroupCount.isVisible =
                        data is AcceptInviteEvent.InviteLoaded && data.data.groupCount != null

                    vb.tvGroup.isVisible = data !is AcceptInviteEvent.InviteEmpty
                    setGroupButtonState(false)

                    vb.btConfirm.setContent(data.data.actionButtonText)
                    vb.tvGroupCount.text = data.data.groupCount?.toString() ?: "0"
                    vb.tvInviteName.text = data.data.name
                    vb.tvInviteImage.text = data.data.imageEmoji
                    vb.tvInviteDescription.text = data.data.description

                    if (data is AcceptInviteEvent.InviteAccepted)
                        findNavController().popBackStack()

                }
                is Payload.Error -> {
                    snackBarError(vb.btConfirm, requireContext())
                }
            }
        }
    }

    private fun setGroupButtonState(error: Boolean) {

        if (vm.canPickGroup()) {
            vb.tvGroup.isEnabled = true
            vb.tvGroup.setCloseIconResource(R.drawable.ic_arrow_filled_down)
        } else {
            vb.tvGroup.isEnabled = false
            vb.tvGroup.closeIcon = null
        }

        if (error) {
            vb.tvGroup.chipStrokeWidth = 1f.dp2px
            vb.tvGroup.setChipStrokeColorResource(R.color.highlightRed)
            vb.tvGroup.setChipBackgroundColorResource(R.color.lightRedError)
        } else if (vm.canPickGroup()) {
            vb.tvGroup.chipStrokeWidth = 1f.dp2px
            vb.tvGroup.setChipStrokeColorResource(R.color.theme_accentLightDark)
            vb.tvGroup.setChipBackgroundColorResource(R.color.accentAlpha20)
        } else {
            vb.tvGroup.chipStrokeWidth = 0f.dp2px
            vb.tvGroup.setChipBackgroundColorResource(R.color.accentAlpha20)
        }
    }

    private fun pickGroup() {
        vm.getAllGroups().observe(viewLifecycleOwner) { groups ->
            BottomSheetDialog.show(
                this,
                GroupPickerFragment(),
                groups.map { GroupItemData(it, GroupItemData.GroupUiType.PICKER) }.toMutableList()
            ) { _, bundle ->
                BottomSheetDialog.getAction(bundle)?.id?.let {
                    vm.setGroup(it)
                }
            }
        }
    }

}