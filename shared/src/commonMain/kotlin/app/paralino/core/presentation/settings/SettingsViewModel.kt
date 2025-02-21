package app.paralino.core.presentation.settings

import app.paralino.core.MR
import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPlans
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val personalCodeRepo: PersonalCodesRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
    private val appRatingManager: AppRatingManager
) : CoreViewModel() {

    private val _accountState = MutableStateFlow<Payload<AccountScreenState>>(
        mapUserState(
            userState = userRepo.localUser.value,
            remoteConfig = remoteConfigRepository.getCachedConfigOnce(),
            personalCode = personalCodeRepo.activePersonalCodeState.value,
            userPlans = userRepo.userPlans.value
        )
    )
    val accountState: StateFlow<Payload<AccountScreenState>> = _accountState

    private val _eventChannel = Channel<AccountScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private var locationBoostTimeJob: Job? = null

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.APP_SETTINGS)

        coroutineScope.launch {
            userRepo.refreshUserMaybe().data?.let {
                try {
                    personalCodeRepo.refreshPersonalCodesAndRestockKeys()
                } catch (_: Exception) { }
            }
        }

        combine(
            userRepo.localUser,
            remoteConfigRepository.getConfigLocally(),
            personalCodeRepo.activePersonalCodeState,
            userRepo.userPlans
        ) { userState, appConfig, personalCode, userPlans ->
            mapUserState(userState, appConfig, personalCode, userPlans)
        }.onEach { value ->
            _accountState.update { value }
            runLocationBoostJob()
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun mapUserState(
        userState: Resource<UserMe?>,
        remoteConfig: ConfigData?,
        personalCode: UserPersonalCode?,
        userPlans: UserPlans
    ): Payload<AccountScreenState> {
        return when (userState) {
            Resource.NotInitialized -> Payload.Loading
            is Resource.Ready -> {
                if (userState.data != null) {
                    Payload.Success(
                        AccountScreenState(
                            userState = UserState(
                                name = userState.data.name,
                                image = userState.data.data?.userImage,
                                email = userState.data.email,
                                personalCode = personalCode?.formattedCode,
                                isGuestUser = userState.data.isGuestUser,
                                errorItem = if (userState.data.needsEmailVerification)
                                    ConditionErrorType.EmailVerification(userState.data.email)
                                else null
                            ),
                            canUseEmail = remoteConfig?.isEmailEnabled ?: false,
                            logoutState = null,
                            deleteAccountState = null,
                            versionName = deviceApiProvider.appVersionText,
                            boostTime = null,
                            placeActivityThrottleEnabled = appPrefs.placeActivityThrottle,
                            userPlan = userPlans.highestValidPlanConfig
                        )
                    )
                } else {
                    Payload.Error(UnauthorizedException(null, type = null, "User is not ready"))
                }
            }
        }
    }

    private fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                messageDispatcher.showSnackbar(MR.strings.info_email_check_instructions_popup.localizedString())
                _eventChannel.send(AccountScreenEvent.EmailVerificationSent)
            } catch (e: Exception) {
                messageDispatcher.showSnackbar(e.errorString())
            }
        }
    }

    private fun logout() {
        flow {
            logoutControl.fullLogout(logoutLocallyOnError = false)
            emit(Unit)
        }
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
        flow {
            logoutControl.deleteAccount(logoutLocallyOnError = false)
            emit(Unit)
        }
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
        _accountState.value.data?.let {
            val boostValue = it.boostTime == null
            logg.v("Toggle manual location boost: $boostValue")

            val trackingBoostEndTime = if (boostValue) {
                nowUTC() + 2.hours
            } else {
                null
            }

            appPrefs.trackingBoostEndTime = trackingBoostEndTime

            if (boostValue) {
                locationPerformanceManager.locationBoostRequested(
                    performanceType = LocationPerformanceType.MANUAL_BOOST,
                    groupId = null,
                    from = "Settings manual boost"
                )
            } else {
                locationPerformanceManager.disableManualBoost()
            }
            runLocationBoostJob()
        }
    }

    private fun togglePlaceActivityThrottle() {
        _accountState.update {
            it.mapSuccess {
                val newValue = !it.placeActivityThrottleEnabled
                logg.v("Toggle place activity throttle: $newValue")
                appPrefs.placeActivityThrottle = newValue
                it.copy(placeActivityThrottleEnabled = newValue)
            }
        }
    }

    private fun onShowDialog(dialog: MainAlertDialog?) {
        _accountState.update { it.mapSuccess { it.copy(alertDialog = dialog) } }
    }

    fun onEvent(event: AccountScreenUiEvent) {
        when (event) {
            AccountScreenUiEvent.DeleteAccount -> onShowDialog(MainAlertDialog.DeleteAccount)
            AccountScreenUiEvent.Logout -> onShowDialog(MainAlertDialog.Logout)
            AccountScreenUiEvent.ToggleLocationBoost -> toggleBoost()
            AccountScreenUiEvent.TogglePlaceActivityThrottle -> togglePlaceActivityThrottle()
            is AccountScreenUiEvent.Copy -> copyToClipboard(event.content)
            AccountScreenUiEvent.SendEmailVerification -> {
                _accountState.update {
                    it.mapSuccess {
                        it.copy(alertDialog = MainAlertDialog.VerifyEmail(it.userState.email))
                    }
                }
            }

            is AccountScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.Logout -> {
                        onShowDialog(null)
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            logout()
                        }
                    }

                    is MainAlertDialog.DeleteAccount -> {
                        when (event.action.action) {
                            MainAlertDialog.Buttons.buttonOk -> {
                                coroutineScope.launch {
                                    onShowDialog(null)
                                    delay(200)
                                    onShowDialog(MainAlertDialog.DeleteAccountConfirm)
                                }
                            }
                            else -> onShowDialog(null)
                        }
                    }

                    is MainAlertDialog.DeleteAccountConfirm -> {
                        onShowDialog(null)
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            deleteAccount()
                        }
                    }

                    is MainAlertDialog.VerifyEmail -> {
                        onShowDialog(null)
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            sendEmailVerification()
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun runLocationBoostJob() {
        locationBoostTimeJob?.cancel()
        locationBoostTimeJob = coroutineScope.launch {
            while (isActive) {
                updateLocationBoostTime()
                delay(1.minutes)
                if (appPrefs.trackingBoostEndTime == null) {
                    locationBoostTimeJob?.cancel()
                }
            }
        }
    }

    private fun updateLocationBoostTime() {
        _accountState.update {
            it.mapSuccess {
                it.copy(
                    boostTime = appPrefs.trackingBoostEndTime?.takeIf { it > nowUTC() }?.let {
                        valueFormatter.formatTimeOnly(it.toLocalDateTime(TimeZone.UTC))
                    }
                )
            }
        }
    }
}