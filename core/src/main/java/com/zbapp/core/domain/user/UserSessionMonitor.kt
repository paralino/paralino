package com.zbapp.core.domain.user

import kotlinx.coroutines.flow.*

class UserSessionMonitor {

    private val sessionStatus = MutableStateFlow(true)

    fun updateSessionState(sessionValid: Boolean) {
        sessionStatus.value = sessionValid
    }

    fun isSessionExpired(): Boolean {
        return !sessionStatus.value
    }

    fun listenForSessionExpired(): Flow<Unit> {
        // only emit if session expired
        return sessionStatus
            .filter { !it }
            .map { Unit }
    }

}