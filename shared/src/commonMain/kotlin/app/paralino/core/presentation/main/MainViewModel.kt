package app.paralino.core.presentation.main

import app.paralino.core.MR
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.ExpiredSessionHandler
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.utils.NetworkQualityMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel constructor(
    private val userStateMonitor: AppAccountStateMonitor,
    private val sessionMonitor: UserSessionMonitor,
    private val networkQualityMonitor: NetworkQualityMonitor,
    private val userRepo: UserRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val expiredSessionHandler: ExpiredSessionHandler,
    private val appUpdateManager: IAppUpdateManager,
    private val premiumStatusManager: PremiumStatusManager,
    private val appPrefs: AppPrefs
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
            .onEach { userState ->
                _uiState.update { it.copy(userState = userState) }
            }
            .map { it.data is AppAccountState.USER_READY }
            .distinctUntilChanged()
            .onEach {
                if (it) {
                    appPrefs.appLaunchedCountWhenLoggedIn += 1
                }
            }
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

        networkQualityMonitor.listenForBadNetwork()
            .onEach {
                networkQualityMonitor.clearLogs()
                messageDispatcher.showSnackbar(MR.strings.error_unstable_connection.localizedString())
            }
            .launchIn(coroutineScope)

        premiumStatusManager.hasGroupOverLimits
            .onEach {
                if (it) {
                    premiumStatusManager.resetLimitState()
                    messageDispatcher.showSnackbar(
                        text = MR.strings.plan_limit_overLimit_user_message.localizedString(),
                        duration = SnackbarDuration.Forever
                    )
                }
            }
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

    fun setDeeplink(deeplinkType: DeeplinkType?) {
        when (deeplinkType) {
            is DeeplinkType.EmailVerify -> {
                verifyEmail(userId = deeplinkType.userId, secret = deeplinkType.secret)
            }
            is DeeplinkType.ParalinoID -> {
                _uiState.update { it.copy(pendingDeeplinkType = deeplinkType) }
            }
            is DeeplinkType.PasswordReset -> {
                _uiState.update { it.copy(pendingDeeplinkType = deeplinkType) }
            }
            null -> {
                _uiState.update { it.copy(pendingDeeplinkType = null) }
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