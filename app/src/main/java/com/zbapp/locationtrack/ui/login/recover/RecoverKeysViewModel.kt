package com.zbapp.locationtrack.ui.login.recover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.domain.user.LogoutUseCase
import com.zbapp.core.domain.user.RecoverAccountUseCase
import com.zbapp.core.presentation.login.recover.RecoverKeysViewModel

class RecoverKeysViewModel constructor(
    logoutUseCase: LogoutUseCase,
    accountUseCase: RecoverAccountUseCase,
) : ViewModel() {

    private val coreVm =
        RecoverKeysViewModel(logoutUseCase, accountUseCase, viewModelScope)

    val uiState = coreVm.uiState
    val events = coreVm.events

    fun recoverAccount() = coreVm.recoverAccount()
    fun resetAccount() = coreVm.resetAccount()
    fun logout() = coreVm.logout()
    fun checkForKey() = coreVm.checkForKey()
    fun onOldPasswordChange(password: String) = coreVm.onOldPasswordChange(password)

}