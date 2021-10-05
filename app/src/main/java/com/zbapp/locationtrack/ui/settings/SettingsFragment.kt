package com.zbapp.locationtrack.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentDashboardBinding
import com.zbapp.locationtrack.databinding.FragmentSettingsBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val vm: SettingsViewModel by viewModel()
    val binding by viewBinding(FragmentSettingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileHeader.clipToOutline = true
        binding.settingHolderGeneral.clipToOutline = true
        binding.settingHolderLocation.clipToOutline = true

        binding.tvAppInfo.text = "v${BuildConfig.VERSION_NAME}"

        binding.profileHeader.setOnClickListener { }
        binding.tvSettingLocation.setOnClickListener { }
        binding.tvSettingAck.setOnClickListener { }
        binding.ivProfileEdit.setOnClickListener { }
        binding.tvLogout.setOnClickListener {
            vm.logout().observe { Toast.makeText(requireContext(), "Logout: $it", Toast.LENGTH_SHORT).show() }
        }

        binding.tvDeleteAccount.setOnClickListener {
            vm.deleteAccount().observe { Toast.makeText(requireContext(), "Delete account: $it", Toast.LENGTH_SHORT).show() }
        }
    }

}