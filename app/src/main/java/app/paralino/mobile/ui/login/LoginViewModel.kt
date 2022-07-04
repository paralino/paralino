package app.paralino.mobile.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.CreateAccountUseCase
import app.paralino.core.domain.user.LoginUseCase
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.login.LoginScreenUiType
import app.paralino.core.presentation.login.LoginViewModel

class LoginViewModel constructor(
    app: Application,
    private val loginUseCase: LoginUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    startScreenUiType: LoginScreenUiType
) : AndroidViewModel(app) {

    private val coreVm = LoginViewModel(loginUseCase, createAccountUseCase, resetPasswordUseCase, startScreenUiType, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()

    fun toggleLoginSignup() = coreVm.toggleLoginSignup()
    fun toggleForgotPassword() = coreVm.toggleForgotPassword()

    fun onUserNameChange(name: String) = coreVm.onUserNameChange(name)
    fun onUserImageChange(image: ImageModel) = coreVm.onUserImageChange(image)
    fun onEmailChange(email: String) = coreVm.onEmailChange(email)
    fun onPasswordChange(password: String) = coreVm.onPasswordChange(password)
    fun onPasswordConfirmChange(password: String) = coreVm.onPasswordConfirmChange(password)
}
