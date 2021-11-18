package com.zbapp.locationtrack.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.locationtrack.BuildConfig
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentDashboardBinding
import com.zbapp.locationtrack.databinding.FragmentSettingsBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
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
        binding.tvSettingAck.setOnClickListener { }
        binding.ivProfileEdit.setOnClickListener { }
        binding.tvLogout.setOnClickListener {
            vm.logout().observe { Toast.makeText(requireContext(), "Logout: $it", Toast.LENGTH_SHORT).show() }
        }

        binding.tvDeleteAccount.setOnClickListener {
            vm.deleteAccount().observe { Toast.makeText(requireContext(), "Delete account: $it", Toast.LENGTH_SHORT).show() }
        }

        binding.tvSettingLocation.setOnClickListener {
            navigateDefault(R.id.acceptInviteFragment)
        }

        binding.tvSettingSharing.setOnClickListener {
            navigateDefault(R.id.manageInviteFragment)
        }

        vm.userData.observe {
            binding.tvLogout.isVisible = it is Payload.Success
            binding.tvDeleteAccount.isVisible = it is Payload.Success
            binding.settingHolderLocation.isVisible = it is Payload.Success

            binding.tvUserName.text = it.data?.name ?: "Your name"
            binding.tvUserInfo.text = it.data?.email ?: "Your email"
            binding.tvUserBadge.text = it.data?.id ?: "Your ID"
        }
    }

}