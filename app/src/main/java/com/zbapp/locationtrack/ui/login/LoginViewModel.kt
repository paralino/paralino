package com.zbapp.locationtrack.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.login.LoginViewModel
import com.zbapp.core.repo.user.LoginManager

class LoginViewModel(
    app: Application,
    private val loginManager: LoginManager,
) : AndroidViewModel(app) {

    private val coreVm = LoginViewModel(loginManager, viewModelScope)

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
