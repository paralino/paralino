package app.paralino.core.presentation.settings

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
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.exceptions.UnauthorizedException
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
            appConfigRepository.getConfig()
        ) { userState, appConfig ->
            _accountState.update {
                when (userState) {
                    Resource.NotInitialized -> it
                    is Resource.Ready -> {
                        if (userState.data != null) {
                            Payload.Success(
                                AccountScreenState(
                                    userState = UserState(
                                        name = userState.data.name,
                                        userId = userState.data.id,
                                        email = userState.data.email,
                                        image = userState.data.data?.userImage,
                                        isGuestUser = userState.data.isGuestUser,
                                        errorItem = if (userState.data.needsEmailVerification)
                                            ConditionErrorType.EMAIL_VERIFICATION(userState.data.email)
                                        else null
                                    ),
                                    canUseEmail = appConfig.emailEnabled,
                                    logoutState = null,
                                    deleteAccountState = null,
                                    versionName = "v${deviceApiProvider.appVersion} (${deviceApiProvider.appVersionCode})",
                                    boostEnabled = config.trackingBoost
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

    fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                _eventChannel.send(AccountScreenEvent.EmailVerificationSent)
            } catch (e: Exception) {
                _eventChannel.send(AccountScreenEvent.Error(e))
            }
        }
    }

    fun logout() {
        logoutControl.logout()
            .mapToPayload { Unit }
            .onEach { state ->
                _accountState.update {
                    it.mapSuccess {
                        _eventChannel.send(state.exception?.let { AccountScreenEvent.Error(it) })
                        it.copy(logoutState = state)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun deleteAccount() {
        logoutControl.deleteAccount()
            .mapToPayload { Unit }
            .onEach { state ->
                _accountState.update {
                    it.mapSuccess {
                        _eventChannel.send(state.exception?.let { AccountScreenEvent.Error(it) })
                        it.copy(deleteAccountState = state)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun copyToClipboard(data: String) {
        deviceApiProvider.copyToClipboard(data)
    }

    fun toggleBoost() {
        _accountState.update {
            it.mapSuccess {
                val boostValue = !it.boostEnabled
                logg.v("Toggle manual location boost: $boostValue")
                config.trackingBoost = boostValue
                if(boostValue) {
                    locationPerformanceManager.locationBoostRequested(groupId = null, LocationPerformanceType.MANUAL_BOOST)
                } else {
                    locationPerformanceManager.disableManualBoost()
                }
                it.copy(boostEnabled = boostValue)
            }
        }
    }
}