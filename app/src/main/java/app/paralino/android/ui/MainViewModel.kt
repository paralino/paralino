package app.paralino.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.presentation.MainViewModel
import app.paralino.core.repo.user.UserRepository

class MainViewModel constructor(
    userStateMonitor: AppAccountStateMonitor,
    sessionMonitor: UserSessionMonitor,
    userRepo: UserRepository,
) : ViewModel() {

    private val coreVm = MainViewModel(userStateMonitor, sessionMonitor, userRepo, viewModelScope)

    val sessionExpiredFlow = coreVm.sessionExpiredFlow
    val userState = coreVm.userState
    val events = coreVm.events

    fun verifyEmail(userId: String?, secret: String?) = coreVm.verifyEmail(userId, secret)

}