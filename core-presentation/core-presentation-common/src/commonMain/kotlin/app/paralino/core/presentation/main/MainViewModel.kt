package app.paralino.core.presentation.main

import app.paralino.core.MR
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.AppAccountState
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.BillingRefreshMonitor
import app.paralino.core.domain.user.ExpiredSessionHandler
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IPlatformUrlHandler
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppUpdateType
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.NetworkQualityMonitor
import app.paralino.core.utils.DeeplinkType
import app.paralino.core.utils.extensions.errorString
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
    private val appPrefs: AppPrefs,
    private val billingRefreshMonitor: BillingRefreshMonitor,
    private val urlHandler: IPlatformUrlHandler
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(MainActivityState())
    val uiState: StateFlow<MainActivityState> = _uiState

    private val _eventChannel = Channel<MainScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        sessionMonitor.listenForFullyExpiredSession()
            .onEach { _uiState.update { it.copy(sessionExpiredDialog = MainAlertDialog.SessionExpired) } }
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

        remoteConfigRepository.listenForNewAppVersionEvents()
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

    fun onEvent(event: MainActivityUiEvent) {
        when (event) {
            is MainActivityUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.AppVersionInfo -> {
                        if (event.action.action == MainAlertDialog.AppVersionInfo.buttonChangelog) {
                            urlHandler.openUrl(BackendResourceIDs.feedbackPortalChangelog)
                        } else if (event.action.isOk) {
                            startAppUpdate()
                        } else if (event.action.isCancel){
                            cancelVersion()
                        }
                    }

                    is MainAlertDialog.SessionExpired -> {
                        _uiState.update { it.copy(sessionExpiredDialog = null) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun cancelVersion() {
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
                    _uiState.update { it.copy(dataRefreshBlockingLoadingShown = true) }
                    userRepo.confirmEmail(userId, secret)
                    userRepo.refreshUserIfAvailable()
                    _uiState.update { it.copy(dataRefreshBlockingLoadingShown = expiredSessionHandler.isProcessingAny()) }
                    showSnackbar(MR.strings.info_email_verified.localizedString())
                } catch (e: Exception) {
                    _uiState.update { it.copy(dataRefreshBlockingLoadingShown = expiredSessionHandler.isProcessingAny()) }
                    showSnackbar(e.errorString())
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

            is DeeplinkType.HomeScreen -> {
                _uiState.update { it.copy(pendingDeeplinkType = null) }
            }

            is DeeplinkType.PaywallScreen -> {
                if (deeplinkType.action == "refresh") {
                    billingRefreshMonitor.triggerRefresh()
                } else {
                    logg.w("billing", "Set billing deeplink")
                    _uiState.update { it.copy(pendingDeeplinkType = deeplinkType) }
                }
            }

            is DeeplinkType.PushNotificationServiceScreen -> {
                _uiState.update { it.copy(pendingDeeplinkType = deeplinkType) }
            }

            null -> {
                _uiState.update { it.copy(pendingDeeplinkType = null) }
            }
        }
    }

    private fun startAppUpdate() {
        when (uiState.value.updateType) {
            is AppUpdateType.FORCE,
            is AppUpdateType.FORCE_AND_CANCEL,
            is AppUpdateType.PASSIVE,
            is AppUpdateType.SOFT_CONSTANT,
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

}