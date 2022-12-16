package app.paralino.core.domain.user

import app.paralino.core.logg
import kotlinx.coroutines.flow.*

class UserSessionMonitor {

    private val sessionStatus = MutableStateFlow(true)

    fun updateSessionState(sessionValid: Boolean) {
        logg.tag("account_session").v("Update session state - is valid: $sessionValid")
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