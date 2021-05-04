package com.zbapp.locationtrack.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zbapp.core.models.Payload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentCreateGroupBinding
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetTitleData
import com.zbapp.locationtrack.ui.shared.emoji.EmojiItemData
import com.zbapp.locationtrack.ui.shared.emoji.EmojiPickerFragment
import com.zbapp.locationtrack.ui.shared.emoji.Emojis
import com.zbapp.locationtrack.utils.extensions.viewBinding
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateGroupFragment: BottomSheetDialogFragment() {

    val vb by viewBinding(FragmentCreateGroupBinding::bind)
    val vm: CreateGroupViewModel by viewModel()

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogStyle
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.title.itemData = BottomSheetTitleData(requireContext().getString(R.string.dashboard_groups_add_button))
        vb.ivGroup.setOnClickListener {
            BottomSheetDialog.show(
                this,
                EmojiPickerFragment(),
                Emojis.groupEmojis.map { EmojiItemData(it) }.toMutableList()
            ) { _, bundle ->
                BottomSheetDialog.getAction(bundle)?.id?.let {
                    vb.tvGroupEmoji.text = it
                }
            }
        }

        vb.bCreate.setOnClickListener {
            vm.createGroup(vb.tvGroupEmoji.text.toString(), vb.etGroupName.text?.toString() ?: "").observe(viewLifecycleOwner) {
                vb.bCreate.isEnabled = it !is Payload.Loading
                vb.bCreate.setLoading(it is Payload.Loading, withCheck = it is Payload.Success)
                if(it is Payload.Success) {
                    lifecycleScope.launchWhenResumed {
                        delay(600)
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

}