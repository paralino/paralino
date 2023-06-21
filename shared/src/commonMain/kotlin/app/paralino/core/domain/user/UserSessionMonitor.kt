package app.paralino.core.domain.user

import app.paralino.core.logg
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.*
import org.koin.ext.getFullName

class UserSessionMonitor {

    private val sessionStatus = MutableStateFlow(true)

    fun processNetworkError(error: Throwable) {
        logg.w("Remote exception - ${error::class.getFullName()} - ${error.message}")
        val unauthorizedException = (error as? UnauthorizedException)
        logg.w("Remote exception - Unauthorized exception (${error is UnauthorizedException}) type: ${unauthorizedException?.type}")
        when (unauthorizedException?.type) {
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