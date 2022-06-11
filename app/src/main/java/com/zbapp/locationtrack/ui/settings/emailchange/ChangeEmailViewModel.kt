package com.zbapp.locationtrack.ui.settings.emailchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.domain.user.ChangeEmailUseCase
import com.zbapp.core.presentation.login.LoginViewModel
import com.zbapp.core.presentation.settings.emailchange.ChangeEmailViewModel
import com.zbapp.core.repo.user.UserRepository

class ChangeEmailViewModel(
    userRepository: UserRepository,
    changeEmailUseCase: ChangeEmailUseCase
): ViewModel() {

    private val coreVm = ChangeEmailViewModel(userRepository, changeEmailUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun proceed() = coreVm.proceed()
    fun onEmailChange(email: String) = coreVm.onEmailChange(email)
    fun onPasswordChange(password: String) = coreVm.onPasswordChange(password)

}