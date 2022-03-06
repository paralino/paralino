package com.zbapp.locationtrack.ui.groups

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.navigation.fragment.findNavController
import com.zbapp.core.utils.SharingDurationPreset
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentSharingStartBinding
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.dashboard.GroupItemData
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.list.ErrorItems
import com.zbapp.locationtrack.ui.shared.list.ErrorListItem
import com.zbapp.locationtrack.ui.shared.list.ErrorListItemType
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration

class SharingStartFragment : BaseFragment(R.layout.fragment_sharing_start) {

    private val vb by viewBinding(FragmentSharingStartBinding::bind)
    private val vm: SharingViewModel by viewModel { parametersOf(arguments?.let { SharingStartFragmentArgs.fromBundle(it) }) }
    private lateinit var permissionHandler: LocationPermissionHandler
    private var animating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler = LocationPermissionHandler(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUi()
        setObservers()
    }

    private fun setUi() {
        vb.timeSeekBar.max = SharingDurationPreset.presets.size - 1
        vb.timeSeekBar.progress = 2
        vb.tvDuration.text = getTime(SharingDurationPreset.presets[vb.timeSeekBar.progress])
        vb.switchBattery.switchText.setText(R.string.groups_sharing_setting_battery_share)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setObservers() {

        vb.timeSeekBar.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (!animating) {
                        animating = true
                        vb.progressBackground.isSelected = true
                        vb.tvDuration.isSelected = true
                        vb.progressFocus.animate().alpha(1f).setDuration(100).start()
                        vb.bShareStart.animate().alpha(0f).setDuration(100).start()
                    }
                }
                else -> {
                    animating = false
                    vb.progressBackground.isSelected = false
                    vb.tvDuration.isSelected = false
                    vb.progressFocus.animate().alpha(0f).setDuration(100).start()
                    vb.bShareStart.animate().alpha(1f).setDuration(100).start()
                }

            }
            false
        }

        vb.groupPicker.setOnClickListener {
            pickGroup()
        }

        vb.bShareStart.setOnClickListener {
            val duration = SharingDurationPreset.presets.getOrNull(vb.timeSeekBar.progress)
                ?: SharingDurationPreset.day
            vm.startSharing(duration).observe {
                if(it) findNavController().popBackStack()
            }
        }

        vb.switchBattery.root.setOnClickListener {
            vb.switchBattery.switchWidget.isChecked = !vb.switchBattery.switchWidget.isChecked
        }

        vb.timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                vb.tvDuration.text = getTime(SharingDurationPreset.presets[progress])
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vb.layoutErrors.itemClick = {
            when(it) {
                ErrorListItemType.GPS -> permissionHandler.askLocationService()
                ErrorListItemType.PERMISSION -> permissionHandler.askLocationPermission()
            }
        }

        vm.selectedGroup.observe {
            vb.groupPicker.itemData = it
            vb.bShareStart.isEnabled = it.id.isNotBlank()
        }

        vm.locationStatus.observe {
            vb.layoutErrors.itemData = ErrorItems(it)
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

        fun getPluralTime(id: Int, value: Long): String {
            return resources.getQuantityString(id, value.toInt(), value.toInt())
        }

        return if (duration.toDays() >= 365) {
            getPluralTime(R.plurals.groups_sharing_duration_type_years, duration.toDays() / 365)
        } else if (duration.toDays() >= 30) {
            getPluralTime(R.plurals.groups_sharing_duration_type_months, duration.toDays() / 30)
        } else if (duration.toDays() >= 1) {
            getPluralTime(R.plurals.groups_sharing_duration_type_days, duration.toDays())
        } else if (duration.toHours() >= 1) {
            getPluralTime(R.plurals.groups_sharing_duration_type_hours, duration.toHours())
        } else if (duration.toMinutes() > 1) {
            getPluralTime(R.plurals.groups_sharing_duration_type_minutes, duration.toMinutes())
        } else {
            getString(R.string.groups_sharing_duration_type_infinite)
        }
    }

    override fun onDestroyView() {
        animating = false
        super.onDestroyView()
    }

}