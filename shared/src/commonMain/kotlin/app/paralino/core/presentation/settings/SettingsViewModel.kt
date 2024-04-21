package app.paralino.core.presentation.settings

import app.paralino.core.MR
import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.exception
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.mapSuccess
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository,
    private val appConfigRepository: AppConfigRepository,
    private val personalCodeRepo: PersonalCodesRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val config: AppConfig
) : CoreViewModel() {

    private val _accountState = MutableStateFlow<Payload<AccountScreenState>>(Payload.Loading)
    val accountState: Flow<Payload<AccountScreenState>> = _accountState

    private val _eventChannel = Channel<AccountScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            userRepo.getAndRefreshUser(),
            appConfigRepository.getConfig(),
            personalCodeRepo.getAndRefreshMyPersonalCodes()
        ) { userState, appConfig, personalCodes ->
            _accountState.update {
                when (userState) {
                    Resource.NotInitialized -> it
                    is Resource.Ready -> {
                        if (userState.data != null) {
                            Payload.Success(
                                AccountScreenState(
                                    userState = UserState(
                                        name = userState.data.name,
                                        image = userState.data.data?.userImage,
                                        email = userState.data.email,
                                        personalCode = personalCodes.firstOrNull { it.isStillValid() }?.formattedCode,
                                        isGuestUser = userState.data.isGuestUser,
                                        errorItem = if (userState.data.needsEmailVerification)
                                            ConditionErrorType.EMAIL_VERIFICATION(userState.data.email)
                                        else null
                                    ),
                                    canUseEmail = appConfig.emailEnabled,
                                    logoutState = null,
                                    deleteAccountState = null,
                                    versionName = "v${deviceApiProvider.appVersion} (${deviceApiProvider.appVersionCode})",
                                    boostEnabled = config.trackingBoost,
                                    placeActivityThrottleEnabled = config.placeActivityThrottle
                                )
                            )
                        } else {
                            Payload.Error(UnauthorizedException(type = null, "User is not ready"))
                        }
                    }
                }
            }
        }
            .launchIn(coroutineScope)
    }

    private fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                messageDispatcher.showSnackbar(MR.strings.info_email_check_instructions.localizedString())
                _eventChannel.send(AccountScreenEvent.EmailVerificationSent)
            } catch (e: Exception) {
                messageDispatcher.showSnackbar(e.errorString())
            }
        }
    }

    private fun logout() {
        logoutControl.logout()
            .mapToPayload { Unit }
            .onEach { state ->
                _accountState.update {
                    it.mapSuccess {
                        state.exception?.let { messageDispatcher.showSnackbar(it.errorString()) }
                        it.copy(logoutState = state)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun deleteAccount() {
        logoutControl.deleteAccount()
            .mapToPayload { Unit }
            .onEach { state ->
                _accountState.update {
                    it.mapSuccess {
                        state.exception?.let { messageDispatcher.showSnackbar(it.errorString()) }
                        it.copy(deleteAccountState = state)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun copyToClipboard(data: String) {
        deviceApiProvider.copyToClipboard(data)
    }

    private fun toggleBoost() {
        _accountState.update {
            it.mapSuccess {
                val boostValue = !it.boostEnabled
                logg.v("Toggle manual location boost: $boostValue")
                config.trackingBoost = boostValue
                if(boostValue) {
                    locationPerformanceManager.locationBoostRequested(
                        performanceType = LocationPerformanceType.MANUAL_BOOST,
                        groupId = null,
                        from = "Settings manual boost"
                    )
                } else {
                    locationPerformanceManager.disableManualBoost()
                }
                it.copy(boostEnabled = boostValue)
            }
        }
    }

    private fun togglePlaceActivityThrottle() {
        _accountState.update {
            it.mapSuccess {
                val newValue = !it.placeActivityThrottleEnabled
                logg.v("Toggle place activity throttle: $newValue")
                config.placeActivityThrottle = newValue
                it.copy(placeActivityThrottleEnabled = newValue)
            }
        }
    }

    private fun onShowDialog(dialog: MainAlertDialog?) {
        _accountState.update { it.mapSuccess { it.copy(alertDialog = dialog) } }
    }

    fun onEvent(event: AccountScreenUiEvent) {
        when (event) {
            AccountScreenUiEvent.ConfirmSendEmailVerification -> sendEmailVerification()
            AccountScreenUiEvent.DeleteAccount -> onShowDialog(MainAlertDialog.DeleteAccount)
            AccountScreenUiEvent.DeleteAccountConfirm -> onShowDialog(MainAlertDialog.DeleteAccountConfirm)
            AccountScreenUiEvent.DeleteAccountSecondConfirm -> deleteAccount()
            AccountScreenUiEvent.HideDialog -> onShowDialog(null)
            AccountScreenUiEvent.Logout -> onShowDialog(MainAlertDialog.Logout)
            AccountScreenUiEvent.LogoutConfirm -> logout()
            AccountScreenUiEvent.ToggleLocationBoost -> toggleBoost()
            AccountScreenUiEvent.TogglePlaceActivityThrottle -> togglePlaceActivityThrottle()
            is AccountScreenUiEvent.Copy -> copyToClipboard(event.content)
            AccountScreenUiEvent.SendEmailVerification -> {
                _accountState.update { it.mapSuccess { it.copy(alertDialog = MainAlertDialog.VerifyEmail(it.userState.email)) } }
            }
        }
    }
}