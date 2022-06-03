package com.zbapp.locationtrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zbapp.core.domain.user.LogoutUseCase
import com.zbapp.core.presentation.settings.SettingsViewModel
import com.zbapp.core.repo.user.UserRepository

// TODO remove when migrated to KMM
class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository
) : ViewModel() {

    private val coreVm = SettingsViewModel(logoutControl, userRepo, viewModelScope)

    val userData = coreVm.accountState
    val events = coreVm.events

    fun logout() = coreVm.logout()

    fun deleteAccount() = coreVm.deleteAccount()
}