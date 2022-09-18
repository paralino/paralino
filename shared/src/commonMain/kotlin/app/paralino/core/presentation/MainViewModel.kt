package app.paralino.core.presentation

import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel constructor(
    private val userStateMonitor: AppAccountStateMonitor,
    private val sessionMonitor: UserSessionMonitor,
    private val userRepo: UserRepository,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    val sessionExpiredFlow = sessionMonitor.listenForSessionExpired()
    val userState = userStateMonitor.userState

    private val _eventChannel = Channel<MainScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    fun verifyEmail(userId: String?, secret: String?) {
        if (!userId.isNullOrBlank() && !secret.isNullOrBlank()) {
            coroutineScope.launch {
                try {
                    userRepo.confirmEmail(userId, secret)
                    userRepo.refreshUserIfAvailable()
                    _eventChannel.send(MainScreenEvent.EmailVerified)
                } catch (e: Exception) {
                    _eventChannel.send(MainScreenEvent.Error(e))
                }
            }
        }
    }

}

sealed class MainScreenEvent {
    data class Error(val error: Throwable) : MainScreenEvent()
    object EmailVerified : MainScreenEvent()
}