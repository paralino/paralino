package com.zbapp.locationtrack.ui.settings

import android.os.Bundle
import android.view.View
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val vm: SettingsViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}