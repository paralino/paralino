package app.paralino.mobile.ui.login.passwordreset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.ResetPasswordUseCase
import app.paralino.core.presentation.login.passwordreset.ResetPasswordViewModel

class ResetPasswordViewModel(
    userId: String,
    secret: String,
    resetPassword: ResetPasswordUseCase,
) : ViewModel() {

    private val coreVm = ResetPasswordViewModel(userId, secret, resetPassword, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onNewPasswordChange(password: String) = coreVm.onNewPasswordChange(password)
    fun onConfirmPasswordChange(password: String) = coreVm.onConfirmPasswordChange(password)
}