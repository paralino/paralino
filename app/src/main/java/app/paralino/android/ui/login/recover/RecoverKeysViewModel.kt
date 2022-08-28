package app.paralino.android.ui.login.recover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.presentation.login.recover.RecoverKeysViewModel

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