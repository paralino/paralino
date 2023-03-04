package app.paralino.core.presentation

import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.Resource
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel constructor(
    private val userStateMonitor: AppAccountStateMonitor,
    private val sessionMonitor: UserSessionMonitor,
    private val userRepo: UserRepository,
    private val appConfigRepository: AppConfigRepository,
) : CoreViewModel() {

    val userState: Flow<Resource<AppAccountState>> = userStateMonitor.userState

    private val _eventChannel = Channel<MainScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        sessionMonitor.listenForSessionExpired()
            .onEach { _eventChannel.send(MainScreenEvent.SessionExpired) }
            .launchIn(coroutineScope)
        appConfigRepository.getUpdateType()
            .onEach { _eventChannel.send(MainScreenEvent.NewAppVersion(updateType = it)) }
            .launchIn(coroutineScope)
    }

    fun cancelVersion() {
        coroutineScope.launch {
            appConfigRepository.cancelNewAppVersion()
        }
    }

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
    object SessionExpired : MainScreenEvent()
    data class NewAppVersion(val updateType: AppUpdateType) : MainScreenEvent()
}