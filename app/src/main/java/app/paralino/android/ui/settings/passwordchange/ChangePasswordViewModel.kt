package app.paralino.android.ui.settings.passwordchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.ChangePasswordUseCase
import app.paralino.core.presentation.settings.passwordchange.ChangePasswordViewModel

class ChangePasswordViewModel(
    changePasswordUseCase: ChangePasswordUseCase
): ViewModel() {

    private val coreVm = ChangePasswordViewModel(changePasswordUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onOldPasswordChange(password: String) = coreVm.onOldPasswordChange(password)
    fun onNewPasswordChange(password: String) = coreVm.onNewPasswordChange(password)
    fun onConfirmPasswordChange(password: String) = coreVm.onConfirmPasswordChange(password)
}