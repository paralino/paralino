package app.paralino.core.domain.user

import app.paralino.core.logg
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.*

class UserSessionMonitor {

    private val sessionStatus = MutableStateFlow(true)

    fun processNetworkError(error: Throwable) {
        logg.w("Remote exception - ${error::class.simpleName} - ${error.message}")
        when ((error as? UnauthorizedException)?.type) {
            "general_unauthorized_scope", "user_blocked" -> {
                // user session is invalid
                logg.w("Remote exception - user session expired")
                updateSessionState(sessionValid = false)
            }
            else -> {}
        }
    }

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