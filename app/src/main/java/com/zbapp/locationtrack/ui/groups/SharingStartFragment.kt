package com.zbapp.locationtrack.ui.groups

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.core.utils.SharingDurationPreset
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentSharingStartBinding
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.shared.bottomsheet.BottomSheetDialog
import com.zbapp.locationtrack.ui.shared.list.ErrorItems
import com.zbapp.locationtrack.ui.shared.list.nameRes
import com.zbapp.locationtrack.ui.shared.snackBarSimple
import com.zbapp.locationtrack.utils.extensions.attributedBold
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration

class SharingStartFragment : BaseFragment(R.layout.fragment_sharing_start) {

    private val vb by viewBinding(FragmentSharingStartBinding::bind)
    private val vm: SharingViewModel by viewModel { parametersOf(arguments?.let { SharingStartFragmentArgs.fromBundle(it) }) }
    private lateinit var permissionHandler: LocationPermissionHandler
    private var animating: Boolean = false

    private val selectedPreset get() = SharingDurationPreset.presets.getOrNull(vb.timeSeekBar.progress)
        ?: SharingDurationPreset.day
    private val isLongTermSharing get() = selectedPreset >= SharingDurationPreset.backgroundAccessThreshold

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
        vb.progressFocus.isClickable = false
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
            when {
//                vm.selectedGroup.value?.id.isNullOrBlank() -> {
//                    snackBarSimple(vb.bShareStart, getString(R.string.invite_connect_alert_pickGroup))
//                }
//                vm.locationStatus.isOK(forAlwaysOnAccess = isLongTermSharing) -> {
//                    enableSharing()
//                }
//                vm.locationStatus.isOK(forAlwaysOnAccess = false) && isLongTermSharing -> {
//                    AlertDialog.Builder(requireContext())
//                        .setTitle(R.string.groups_sharing_potential_error_title)
//                        .setMessage(getString(R.string.groups_sharing_potential_error_background_location_message).attributedBold(requireContext()))
//                        .setPositiveButton(R.string.button_ok) { _, _ ->
//                            permissionHandler.askBackgroundLocationPermission()
//                        }
//                        .setNegativeButton(R.string.button_skip) { _, _ ->
//                            enableSharing()
//                        }
//                        .show()
//                }
//                else -> {
//                    val errorSection = ErrorItems(vm.locationStatus).list.map { getString(it.nameRes) }
//                        .joinToString(separator = "**\n- **", prefix = "- **", postfix = "**")
//                    AlertDialog.Builder(requireContext())
//                        .setTitle(R.string.groups_sharing_potential_error_title)
//                        .setMessage(getString(R.string.groups_sharing_potential_error_message, errorSection).attributedBold(requireContext()))
//                        .setPositiveButton(R.string.groups_sharing_potential_error_button_enable) { _, _ ->
//                            enableSharing()
//                        }
//                        .setNegativeButton(R.string.button_cancel, null)
//                        .show()
//                }
            }
        }

        vb.switchBattery.root.setOnClickListener {
            vb.switchBattery.switchWidget.isChecked = !vb.switchBattery.switchWidget.isChecked
        }

//        vb.timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                setupSharingStartButton(vm.locationStatus)
//                vb.tvDuration.text = getTime(SharingDurationPreset.presets[progress])
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//        })

        vb.layoutErrors.itemClick = {
            when(it) {
                ConditionErrorType.GPS -> permissionHandler.askLocationService()
                ConditionErrorType.PERMISSION_LOCATION -> permissionHandler.askLocationPermission()
                ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> permissionHandler.askBackgroundLocationPermission()
            }
        }

//        vm.selectedGroup.observe {
//            vb.groupPicker.itemData = it
//        }
//
//        vm.locationStatusLiveData.observe {
//            setupSharingStartButton(it)
//            vb.layoutErrors.itemData = ErrorItems(it)
//        }
    }

    private fun setupSharingStartButton(status: LocationConditionStatus) {
        vb.bShareStart.setText(
            if(status.isOK()) R.string.groups_sharing_button_start
            else R.string.groups_sharing_button_enable
        )
        vb.bShareStart.setIconResource(
            if(status.isOK(forAlwaysOnAccess = isLongTermSharing)) R.drawable.ic_pin_sharing
            else R.drawable.ic_pin_unknown
        )
    }

    private fun enableSharing() {
//        vm.startSharing(selectedPreset).observe {
//            if(it) findNavController().popBackStack()
//        }
    }

    private fun pickGroup() {
//        vm.getAllGroups().observe { groups ->
//            BottomSheetDialog.show(
//                this,
//                GroupPickerFragment(),
//                groups.map { GroupItemData(it, GroupItemData.GroupUiType.PICKER) }.toMutableList()
//            ) { _, bundle ->
//                BottomSheetDialog.getAction(bundle)?.id?.let {
//                    vm.setSelectedGroup(it)
//                }
//            }
//        }
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