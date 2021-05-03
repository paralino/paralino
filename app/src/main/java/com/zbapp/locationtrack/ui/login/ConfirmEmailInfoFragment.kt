package com.zbapp.locationtrack.ui.login

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.zbapp.core.models.Payload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.ElementGenericInfoBinding
import com.zbapp.locationtrack.databinding.FragmentGenericInfoBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.shared.AppSnackbar
import com.zbapp.locationtrack.utils.extensions.viewBinding
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfirmEmailInfoFragment: BaseFragment(R.layout.fragment_generic_info) {

    private val vb by viewBinding(FragmentGenericInfoBinding::bind)
    private val vbContent by viewBinding { ElementGenericInfoBinding.bind(vb.root) }
    private val vm: LoginCheckViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vbContent.tvInfo.setText(R.string.info_email_confirm)
        vbContent.bAction.setText(R.string.button_done)

        vbContent.bAction.setOnClickListener {
            vm.checkAndSync().observe {
                vbContent.bAction.isEnabled = it !is Payload.Loading
                vbContent.bAction.setLoading(loading = it is Payload.Loading, withCheck = it is Payload.Success)
                if(it is Payload.Success)
                    lifecycleScope.launchWhenResumed {
                        delay(600)
                        findNavController().popBackStack()
                    }
            }
        }

        vm.errorState.observe {
            AppSnackbar.make(vbContent.bAction, it)?.show()
        }
    }

}