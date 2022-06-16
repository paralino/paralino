package com.zbapp.locationtrack.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.domain.user.CreateAccountUseCase
import com.zbapp.core.domain.user.LoginUseCase
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.login.LoginViewModel

class LoginViewModel(
    app: Application,
    private val loginUseCase: LoginUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
) : AndroidViewModel(app) {

    private val coreVm = LoginViewModel(loginUseCase, createAccountUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()

    fun toggleLoginSignup() = coreVm.toggleLoginSignup()
    fun toggleForgotPassword() = coreVm.toggleForgotPassword()

    fun onUserNameChange(name: String) = coreVm.onUserNameChange(name)
    fun onUserImageChange(image: ImageModel) = coreVm.onUserImageChange(image)
    fun onEmailChange(email: String) = coreVm.onEmailChange(email)
    fun onPasswordChange(password: String) = coreVm.onPasswordChange(password)
}
