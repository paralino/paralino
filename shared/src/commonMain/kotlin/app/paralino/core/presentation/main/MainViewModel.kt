package app.paralino.core.presentation.main

import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.ExpiredSessionHandler
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel constructor(
    private val userStateMonitor: AppAccountStateMonitor,
    private val sessionMonitor: UserSessionMonitor,
    private val userRepo: UserRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val expiredSessionHandler: ExpiredSessionHandler,
    private val appUpdateManager: IAppUpdateManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(MainActivityState())
    val uiState: StateFlow<MainActivityState> = _uiState

    private val _eventChannel = Channel<MainScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        sessionMonitor.listenForFullyExpiredSession()
            .onEach { _uiState.update { it.copy(isSessionExpiredShown = true) } }
            .launchIn(coroutineScope)

        userStateMonitor.userState
            .onEach { userState -> _uiState.update { it.copy(userState = userState) } }
            .launchIn(coroutineScope)

        remoteConfigRepository.getUpdateType()
            .onEach { updateType -> _uiState.update { it.copy(updateType = updateType) } }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        appUpdateManager.updateState
            .onEach { updateState -> _uiState.update { it.copy(appUpdateStatus = updateState) } }
            .launchIn(coroutineScope)

        expiredSessionHandler.observeProcessing()
            .onEach { state -> _uiState.update { it.copy(dataRefreshBlockingLoadingShown = state) } }
            .launchIn(coroutineScope)
    }

    fun cancelVersion() {
        coroutineScope.launch {
            remoteConfigRepository.cancelNewAppVersion()
            if (_uiState.value.updateType?.isForced != true) {
                _uiState.update { it.copy(updateType = null) }
            }
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

    fun cancelSessionExpired() {
        _uiState.update { it.copy(isSessionExpiredShown = false) }
    }

    fun startAppUpdate() {
        when (uiState.value.updateType) {
            is AppUpdateType.FORCE,
            is AppUpdateType.FORCE_AND_CANCEL,
            is AppUpdateType.SOFT -> {
                uiState.value.updateType?.configData?.let {
                    appUpdateManager.startUpdateProcess(it)
                }
            }

            is AppUpdateType.NONE,
            null -> {
            }
        }
    }
}

sealed class MainScreenEvent {
    data class Error(val error: Throwable) : MainScreenEvent()
    object EmailVerified : MainScreenEvent()
}