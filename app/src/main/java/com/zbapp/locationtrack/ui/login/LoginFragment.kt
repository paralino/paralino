package com.zbapp.locationtrack.ui.login

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.zbapp.core.models.Payload
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentLoginBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.base.navigateDefault
import com.zbapp.locationtrack.ui.shared.AppSnackbar
import com.zbapp.locationtrack.utils.extensions.viewBinding
import kotlinx.coroutines.delay
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment: BaseFragment(R.layout.fragment_login) {

    private val vm: LoginViewModel by viewModel()
    private val vb by viewBinding(FragmentLoginBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.tvForgotPassword.setOnClickListener { vm.toggleForgotPassword() }
        vb.tvSignUpLogin.setOnClickListener { vm.toggleLoginSignup() }

        vb.bLogin.setOnClickListener {
            onActionButton()
        }

        vb.etUsername.setEndIconOnClickListener {
            vm.generateUsername()
        }

        vm.randomUsername.observe {
            vb.etUsername.editText?.setText(it)
        }

        vm.loginStateValidation.observe {
            val backgroundRes = R.drawable.background_input
            val errorBackgroundRes = R.drawable.background_input_error

            vb.etUsername.setBackgroundResource(if(it is LoginValidation.USERNAME) errorBackgroundRes else backgroundRes)
            vb.etEmail.setBackgroundResource(if(it is LoginValidation.EMAIL) errorBackgroundRes else backgroundRes)
            vb.etPassword.setBackgroundResource(if(it is LoginValidation.PASSWORD) errorBackgroundRes else backgroundRes)
//            vb.etPasswordConfirm.setBackgroundResource(if(it is LoginValidation.CONFIRM_PASSWORD) errorBackgroundRes else backgroundRes)
        }

        vm.errorState.observe {
            AppSnackbar.make(vb.bLogin, it)?.show()
        }

        vm.loginScreenState.observe {
            vb.tvForgotPassword.isVisible = it == LoginState.LOGIN
            vb.etUsername.isVisible = it == LoginState.SIGNUP
            vb.etPassword.isVisible = it != LoginState.FORGOT_PASSWORD
            when(it) {
                LoginState.LOGIN -> {
                    vb.etUsername.editText?.setText("")
                    vb.etEmail.editText?.setHint(R.string.login_placeholder_email_username)
                    vb.bLogin.setText(R.string.login_signup_switch_button_login)
                    vb.tvSignUpLogin.setText(R.string.login_signup_switch_button_signup)
                }
                LoginState.SIGNUP -> {
                    vb.etEmail.editText?.setHint(R.string.login_placeholder_email)
                    vb.bLogin.setText(R.string.login_button_create_account)
                    vb.tvSignUpLogin.setText(R.string.login_signup_switch_button_login)
                }
                LoginState.FORGOT_PASSWORD -> {
                    vb.etPassword.editText?.setText("")
                    vb.etUsername.editText?.setText("")
                    vb.etEmail.editText?.setHint(R.string.login_placeholder_email_username)
                    vb.bLogin.setText(R.string.login_button_reset_password)
                    vb.tvSignUpLogin.setText(R.string.login_signup_switch_button_login)
                }
            }
        }
    }

    private fun onActionButton() {
        vm.loginOrSignUp(
            username = vb.etUsername.editText?.text?.toString() ?: "",
            email = vb.etEmail.editText?.text?.toString() ?: "",
            password = vb.etPassword.editText?.text?.toString() ?: ""
        ).observe {
            vb.bLogin.isEnabled = it !is Payload.Loading
            vb.bLogin.setLoading(it is Payload.Loading, it is Payload.Success)

            when(it) {
                is Payload.Success -> {
                    lifecycleScope.launchWhenResumed {
                        delay(600)
                        if(vm.isUserVerified()) {
                            findNavController().popBackStack()
                        } else {
                            navigateDefault(R.id.toEmailConfirmInfo)
                        }
                    }
                }
                else -> { }
            }
        }
    }
}