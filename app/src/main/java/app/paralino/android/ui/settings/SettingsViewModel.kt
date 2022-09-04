package app.paralino.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.repo.user.UserRepository

// TODO remove when migrated to KMM
class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository
) : ViewModel() {

    private val coreVm = SettingsViewModel(logoutControl, userRepo, viewModelScope)

    val userData = coreVm.accountState
    val events = coreVm.events

    fun sendEmailVerification() = coreVm.sendEmailVerification()
    fun logout() = coreVm.logout()
    fun deleteAccount() = coreVm.deleteAccount()
}