package com.zbapp.locationtrack.ui.groups.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zbapp.core.models.Payload
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentGroupCreateBinding
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetTitleData
import com.zbapp.locationtrack.ui.shared.emoji.EmojiItemData
import com.zbapp.locationtrack.ui.shared.emoji.EmojiPickerFragment
import com.zbapp.locationtrack.ui.shared.snackBarError
import com.zbapp.locationtrack.ui.shared.snackBarSimple
import com.zbapp.locationtrack.utils.extensions.viewBinding
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CreateGroupFragment : BottomSheetDialogFragment() {

    val vb by viewBinding(FragmentGroupCreateBinding::bind)
    private val args by lazy { arguments?.let { CreateGroupFragmentArgs.fromBundle(it) } }
    val vm: CreateGroupViewModel by viewModel { parametersOf(args?.groupId) }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogStyle
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_group_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUi()
//        setObservers()
    }

    private fun setUi() {
        vb.tvGroupEmoji.clipToOutline = true
        vb.tvGroupEmoji.text = Emojis.group1.random()
    }
//
//    private fun setObservers() {
//
//        vm.groupDetails.observe(viewLifecycleOwner) {
//            when (it) {
//                Payload.Loading -> {}
//                is Payload.Success -> {
//                    vb.title.itemData =
//                        BottomSheetTitleData(requireContext().getString(R.string.group_edit_title))
//                    vb.tvGroupEmoji.text = it.data.image.getImageValue()
//                    vb.etGroupName.editText?.setText(it.data.name)
//                    vb.bCreate.setText(R.string.button_save)
//                }
//                is Payload.Error -> {
//                    vb.title.itemData =
//                        BottomSheetTitleData(requireContext().getString(R.string.group_create_title))
//                    vb.tvGroupEmoji.text = Emojis.group1.random()
//                    vb.bCreate.setText(R.string.group_create_button_text)
//                }
//            }
//        }
//
//        vb.tvGroupEmoji.setOnClickListener {
//            BottomSheetDialog.show(
//                this,
//                EmojiPickerFragment(),
//                Emojis.allEmojis.map { EmojiItemData(it) }.toMutableList()
//            ) { _, bundle ->
//                BottomSheetDialog.getAction(bundle)?.id?.let {
//                    vb.tvGroupEmoji.text = it
//                }
//            }
//        }
//
//        vb.bCreate.setOnClickListener {
//
//            if (vb.etGroupName.editText?.text?.isBlank() == true) {
//                vb.etGroupName.setBackgroundResource(R.drawable.background_input_error)
//                snackBarSimple(
//                    vb.root,
//                    requireContext().getString(R.string.form_generic_validation)
//                )
//                return@setOnClickListener
//            } else {
//                vb.etGroupName.setBackgroundResource(R.drawable.background_input)
//            }
//
//            vm.createGroup(
//                vb.tvGroupEmoji.text.toString(),
//                vb.etGroupName.editText?.text?.toString() ?: ""
//            ).observe(viewLifecycleOwner) {
//                vb.bCreate.isEnabled = it !is Payload.Loading
//                vb.bCreate.setLoading(it is Payload.Loading, withCheck = it is Payload.Success)
//                if (it is Payload.Success) {
//                    lifecycleScope.launchWhenResumed {
//                        delay(300)
//                        dismissAllowingStateLoss()
//                    }
//                } else if (it is Payload.Error) {
//                    snackBarError(vb.root, it.exception)
//                }
//            }
//        }
//    }

}