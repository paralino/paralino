package com.zbapp.locationtrack.ui.settings.passwordchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.domain.user.ChangePasswordUseCase
import com.zbapp.core.presentation.settings.passwordchange.ChangePasswordViewModel

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