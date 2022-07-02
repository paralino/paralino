package com.zbapp.locationtrack.ui

import androidx.lifecycle.ViewModel
import com.zbapp.core.domain.user.AppAccountStateMonitor
import com.zbapp.core.domain.user.UserSessionMonitor

class MainViewModel constructor(
    private val userStateMonitor: AppAccountStateMonitor,
    private val sessionMonitor: UserSessionMonitor,
) : ViewModel() {

    val sessionExpiredFlow = sessionMonitor.listenForSessionExpired()
    val userState = userStateMonitor.userState

}