package app.paralino.core.domain.user

import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.*
import org.koin.ext.getFullName

class UserSessionMonitor {

    companion object {
        val unauthorizedTypes: List<String> = listOf(
            "general_unauthorized_scope", "user_blocked"
        )
    }

    private val sessionStatus = MutableStateFlow<Resource<Boolean>>(Resource.NotInitialized)

    fun processNetworkError(error: Throwable) {
        logg.w("Remote exception - ${error::class.getFullName()} - ${error.message}")
        val unauthorizedException = (error as? UnauthorizedException)
        logg.w("Remote exception - Unauthorized exception (${error is UnauthorizedException}) type: ${unauthorizedException?.type}")
        when {
            unauthorizedTypes.any { it == unauthorizedException?.type } -> {
                // user session is invalid
                logg.w("Remote exception - user session expired")
                updateSessionState(sessionValid = false)
            }
            else -> {}
        }
    }

    fun resetSessionState() {
        sessionStatus.value = Resource.NotInitialized
    }

    fun updateSessionState(sessionValid: Boolean) {
        if (sessionValid) {
            logg.tag("account_session").v("Update session state - is valid: $sessionValid")
            sessionStatus.value = Resource.Ready(true)
        } else if (sessionStatus.value is Resource.Ready) {
            logg.tag("account_session").v("Update session state - is valid: $sessionValid")
            sessionStatus.value = Resource.Ready(false)
        }
    }

    fun isSessionExpired(): Boolean {
        return sessionStatus.value.data == false
    }

    fun listenForSessionExpired(): Flow<Unit> {
        // only emit if session expired
        return sessionStatus
            .filter { it.data == false }
            .map { Unit }
    }

}