package com.zbapp.locationtrack.ui.login

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.databinding.FragmentLoginBinding
import com.zbapp.locationtrack.ui.base.BaseFragment
import com.zbapp.locationtrack.ui.shared.AppSnackbar
import com.zbapp.locationtrack.utils.extensions.viewBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment: BaseFragment(R.layout.fragment_login) {

    private val vm: LoginViewModel by viewModel()
    private val vb by viewBinding(FragmentLoginBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.tvForgotPassword.setOnClickListener { vm.toggleForgotPassword() }
        vb.tvSignUpLogin.setOnClickListener { vm.toggleLoginSignup() }

        vb.bLogin.setOnClickListener {
            vm.loginOrSignUp(
                email = vb.etEmail.editText?.text?.toString() ?: "",
                password = vb.etPassword.editText?.text?.toString() ?: "",
                confirmPassword = vb.etPasswordConfirm.editText?.text?.toString() ?: ""
            ).observe {  }
        }

        vm.loginStateValidation.observe {
            val backgroundRes = R.drawable.background_input
            val errorBackgroundRes = R.drawable.background_input_error

            vb.etEmail.setBackgroundResource(if(it is LoginValidation.EMAIL) errorBackgroundRes else backgroundRes)
            vb.etPassword.setBackgroundResource(if(it is LoginValidation.PASSWORD) errorBackgroundRes else backgroundRes)
            vb.etPasswordConfirm.setBackgroundResource(if(it is LoginValidation.CONFIRM_PASSWORD) errorBackgroundRes else backgroundRes)
        }

        vm.errorState.observe {
            AppSnackbar.make(vb.bLogin, it)?.show()
        }

        vm.loginState.observe {
            vb.tvForgotPassword.isVisible = it == LoginState.LOGIN
            vb.etPassword.isVisible = it != LoginState.FORGOT_PASSWORD
            vb.etPasswordConfirm.isVisible = it == LoginState.SIGNUP
            when(it) {
                LoginState.LOGIN -> {
                    vb.etPasswordConfirm.editText?.setText("")
                    vb.bLogin.setText(R.string.login_signup_switch_button_login)
                    vb.tvSignUpLogin.setText(R.string.login_signup_switch_button_signup)
                }
                LoginState.SIGNUP -> {
                    vb.bLogin.setText(R.string.login_button_create_account)
                    vb.tvSignUpLogin.setText(R.string.login_signup_switch_button_login)
                }
                LoginState.FORGOT_PASSWORD -> {
                    vb.etPassword.editText?.setText("")
                    vb.etPasswordConfirm.editText?.setText("")
                    vb.bLogin.setText(R.string.login_button_reset_password)
                    vb.tvSignUpLogin.setText(R.string.login_signup_switch_button_login)
                }
            }
        }
    }
}