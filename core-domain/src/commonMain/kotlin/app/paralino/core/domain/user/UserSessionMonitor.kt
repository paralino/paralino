package app.paralino.core.domain.user

import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class UserSessionMonitor {

    companion object {
        const val userBlocked: String = "user_blocked"
        const val existingSessionError: String = "user_session_already_exists"
        const val invalidCredentials: String = "user_invalid_credentials"

        val unauthorizedTypes: List<String> = listOf(
            "general_unauthorized_scope", userBlocked
        )
    }

    private val _sessionStatus = MutableStateFlow<Resource<UserSessionState>>(Resource.NotInitialized)
    val sessionStatus = _sessionStatus.asStateFlow()

    fun processNetworkError(error: Throwable) {
        logg.w("Remote exception - ${error::class.simpleName} - ${error.message}")
        val unauthorizedException = (error as? UnauthorizedException)
        logg.w("Remote exception - Unauthorized exception (${error is UnauthorizedException}) type: ${unauthorizedException?.type}")
        when {
            unauthorizedTypes.any { it == unauthorizedException?.type } -> {
                // user session is invalid
                logg.w("Remote exception - user session expired")
                updateSessionState(state = UserSessionState.MAYBE_EXPIRED)
            }

            else -> {}
        }
    }

    fun resetSessionState() {
        _sessionStatus.value = Resource.NotInitialized
    }

    fun updateSessionState(state: UserSessionState) {
        logg.tag("account_session").v("Update session state from ${sessionStatus.value} to $state")
        if (state == UserSessionState.OK) {
            _sessionStatus.value = Resource.Ready(UserSessionState.OK)
        } else if (_sessionStatus.value is Resource.Ready) {
            _sessionStatus.value = Resource.Ready(state)
        }
    }

    fun isSessionValid(): Boolean {
        return _sessionStatus.value.data == UserSessionState.OK
    }

    fun listenForFullyExpiredSession(): Flow<Unit> {
        // only emit if session expired
        return _sessionStatus
            .filter { it.data == UserSessionState.EXPIRED }
            .map { Unit }
    }

    fun listenForSessionExpired(): Flow<Unit> {
        // only emit if session expired
        return _sessionStatus
            .filter { it.data?.isExpired == true }
            .map { Unit }
    }
}

enum class UserSessionState {
    OK, MAYBE_EXPIRED, EXPIRED;

    val isExpired: Boolean get() = this == MAYBE_EXPIRED || this == EXPIRED
}