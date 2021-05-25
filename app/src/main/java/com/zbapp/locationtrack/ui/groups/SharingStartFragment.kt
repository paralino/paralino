package com.zbapp.locationtrack.ui.groups

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.navigation.fragment.findNavController
import com.zbapp.core.utils.SharingDurationPreset
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentSharingStartBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.dashboard.GroupItemData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Duration

class SharingStartFragment: BaseFragment(R.layout.fragment_sharing_start) {

    private val vb by viewBinding(FragmentSharingStartBinding::bind)
    private val vm: SharingViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.ivClose.setOnClickListener { findNavController().popBackStack() }
        vb.groupPicker.setOnClickListener {
            pickGroup()
        }

        vb.timeSeekBar.max = SharingDurationPreset.presets.size - 1
        vb.timeSeekBar.progress = 2
        vb.timeSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                vb.tvDuration.text = getTime(SharingDurationPreset.presets[progress])
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vm.selectedGroup.observe {
            vb.groupPicker.itemData = it
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
                    vm.setSelectedGroup(it)
                }
            }
        }
    }

    fun getTime(milis: Long): String {
        val duration = Duration.ofMillis(milis)
        return if(duration.toDays() >= 365) {
            "${duration.toDays() / 365} years"
        } else if(duration.toDays() >= 30) {
            "${duration.toDays() / 30} months"
        } else if(duration.toDays() >= 1) {
            "${duration.toDays()} days"
        } else if(duration.toHours() >= 1) {
            "${duration.toHours()} hours"
        } else if(duration.toMinutes() > 1) {
            "${duration.toMinutes()} minutes"
        } else {
            "infinite"
        }
    }

}