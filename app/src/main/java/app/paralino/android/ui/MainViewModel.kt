package app.paralino.android.ui

import androidx.lifecycle.ViewModel
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.UserSessionMonitor

class MainViewModel constructor(
    private val userStateMonitor: AppAccountStateMonitor,
    private val sessionMonitor: UserSessionMonitor,
) : ViewModel() {

    val sessionExpiredFlow = sessionMonitor.listenForSessionExpired()
    val userState = userStateMonitor.userState

}