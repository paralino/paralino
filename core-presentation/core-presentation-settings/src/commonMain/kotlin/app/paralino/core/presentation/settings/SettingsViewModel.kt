package app.paralino.core.presentation.settings

import app.paralino.core.MR
import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.PremiumStatusManager
import app.paralino.core.logg
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.user.PushProviderEndpointConfig
import app.paralino.core.models.user.UserConfigModel
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPlans
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.combine
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.mapToPayload
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import kotlin.time.Duration.Companion.seconds

class SettingsViewModel(
    private val logoutControl: LogoutUseCase,
    private val userRepo: UserRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val personalCodeRepo: PersonalCodesRepository,
    val deviceApiProvider: IDeviceApiProvider,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val premiumStatusManager: PremiumStatusManager,
    private val valueFormatter: IValueFormatter,
    private val appPrefs: AppPrefs,
    private val appConfig: AppConfigRepository,
    private val appRatingManager: AppRatingManager,
    private val appUpdateManager: IAppUpdateManager,
    private val accountStateMonitor: AppAccountStateMonitor,
    private val userConfigRepo: UserConfigRepository,
    private val personalSpaceLocal: UserPersonalSpaceLocalDataSource,
    private val notificationServiceManager: NotificationSocketServiceManager,
) : CoreViewModel() {

    private val _accountState = MutableStateFlow<Payload<AccountScreenState>>(
        mapUserState(
            userState = userRepo.localUser.value,
            remoteConfig = remoteConfigRepository.getCachedConfigOnce(),
            personalCode = personalCodeRepo.activePersonalCodeState.value,
            userPlans = userRepo.userPlans.value,
            deviceStatus = deviceApiProvider.currentDeviceStatus(),
            userConfig = userConfigRepo.configState.value,
            personalSpace = personalSpaceLocal.personalSpaceState.value.data,
            previousState = null,
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
            try {
                premiumStatusManager.adjustDeviceSettingsForPlan()
            } catch (e: Exception) {
            }
        }

        combine(
            userRepo.localUser,
            remoteConfigRepository.getConfigLocally(),
            personalCodeRepo.activePersonalCodeState,
            userRepo.userPlans,
            deviceApiProvider.deviceStatus,
            userConfigRepo.configState,
            personalSpaceLocal.personalSpaceState,
        ) { userState, appConfig, personalCode, userPlans, deviceStatus, userConfig, personalSpace ->
            mapUserState(
                userState = userState,
                remoteConfig = appConfig,
                personalCode = personalCode,
                userPlans = userPlans,
                deviceStatus = deviceStatus,
                userConfig = userConfig,
                personalSpace = personalSpace.data,
                previousState = _accountState.value.data
            )
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
        userPlans: UserPlans,
        deviceStatus: DeviceStatusData,
        userConfig: UserConfigModel,
        personalSpace: UserPersonalSpaceModel?,
        previousState: AccountScreenState?,
    ): Payload<AccountScreenState> {
        return when (userState) {
            Resource.NotInitialized -> Payload.Loading
            is Resource.Ready -> {
                val userStateData = userState.data
                if (userStateData != null) {
                    Payload.Success(
                        AccountScreenState(
                            userState = UserState(
                                name = userStateData.name,
                                image = userStateData.data?.userImage,
                                email = userStateData.email,
                                personalCode = personalCode?.formattedCode,
                                isGuestUser = userStateData.isGuestUser
                            ),
                            canUseEmail = remoteConfig?.isEmailEnabled ?: false,
                            logoutState = null,
                            deleteAccountState = null,
                            versionName = deviceApiProvider.appVersionText,
                            boostTime = null,
                            userPlan = userPlans.highestValidPlanConfig,
                            canBoostLocation = appConfig.isInAppDevMode,
                            canAccessDebugScreen = appConfig.isInAppDevMode,
                            appReviewUrl = when (deviceApiProvider.deviceType) {
                                DeviceType.Android -> BackendResourceIDs.storeUrlGooglePlay
                                DeviceType.iOS -> BackendResourceIDs.storeUrlDeeplinkAppleAppStoreReview
                            },
                            isAppReviewExpanded = previousState?.isAppReviewExpanded ?: false,
                            isAppReviewVisible = when (appConfig.appFlavor) {
                                AppFlavor.APPLE_IOS,
                                AppFlavor.ANDROID_GOOGLE -> !appPrefs.wasAppReviewOpened
                                AppFlavor.ANDROID_DIRECT -> false
                            },
                            email = userStateData.email.takeIf { !userStateData.isGuestUser },
                            userId = userStateData.id.ifBlank { appPrefs.userId },
                            billingId = userConfig.billingCustomerId.orEmpty(),
                                deviceId = appPrefs.deviceId,
                                diagnosticsId = appPrefs.errorReportingID,
                            pushToken = formatPushTokenInfo(
                                pushProviderConfig = personalSpace?.currentDevice?.pushProviderConfig
                            ),
                            memberSinceDateFormatted = formatMemberSinceDate(userStateData.registration),
                            errors = buildList {
                                if (userStateData.canVerifyEmail) {
                                    add(
                                        ConditionErrorType.EmailVerification(
                                            email = userStateData.email,
                                            isLoading = previousState?.errors
                                                ?.filterIsInstance<ConditionErrorType.EmailVerification>()
                                                ?.firstOrNull()
                                                ?.isLoading
                                                ?: false
                                        )
                                    )
                                }

                                if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                                    add(ConditionErrorType.PermissionNotifications)
                                }

                                if (remoteConfigRepository.getNewAppVersionUpdateType().isNewVersionAvailable) {
                                    add(ConditionErrorType.NewAppUpdate)
                                }
                            }
                        )
                    )
                } else {
                    Payload.Error(UnauthorizedException(null, type = null, "User is not ready"))
                }
            }
        }
    }

    private fun formatPushTokenInfo(pushProviderConfig: String?): String? {
        if (!appConfig.isInAppDevMode) return null

        val pushConfig = PushProviderEndpointConfig.get(pushProviderConfig)
        val providerInfo = when (notificationServiceManager.adjustedPushProvider()) {
            PushProvider.NOT_SET -> "not_set"
            PushProvider.DEVICE_NATIVE -> "Native"
            PushProvider.PARALINO -> "Paralino"
            PushProvider.UNIFIED_PUSH -> pushConfig?.distributor ?: "UnifiedPush"
        }
        return listOf(
            providerInfo,
            pushConfig?.let { it.id ?: it.url } ?: appPrefs.pushTokenSynced
        ).joinToString(separator = "\n")
    }

    private fun formatMemberSinceDate(registration: String): String {
        val dateTimeUTC = registration.isoWithOffsetToInstant()?.toLocalDateTime(TimeZone.UTC)
            ?: registration.isoToLocalDateTime()
            ?: return ""

        return valueFormatter.formatBestPattern(
            timeUTC = dateTimeUTC,
            pattern = "MMMMdyyyy"
        )
    }

    private fun sendEmailVerification() {
        val isEmailSendingLoading = _accountState.value.data?.errors
            ?.filterIsInstance<ConditionErrorType.EmailVerification>()
            ?.firstOrNull()
            ?.isLoading
            ?: false

        if (isEmailSendingLoading) {
            return
        }

        coroutineScope.launch {
            try {
                // first invalidate skip time so verification screen can be shown
                appPrefs.emailVerificationSkipTime = null
                setEmailVerificationLoading(isLoading = true)
                userRepo.initEmailVerification()
                messageDispatcher.showSnackbar(MR.strings.info_email_check_instructions_popup.localizedString())
                setEmailVerificationLoading(isLoading = false)

                // a bit of delay for the ui
                // not a big issue if screen is gone in the meantime, verification will popup again
                delay(1.seconds)
                accountStateMonitor.triggerStateCheck()
                _eventChannel.send(AccountScreenEvent.EmailVerificationSent)
            } catch (e: Exception) {
                setEmailVerificationLoading(isLoading = false)
                messageDispatcher.showSnackbar(e.errorString())
                accountStateMonitor.triggerStateCheck()
            }
        }
    }

    private fun setEmailVerificationLoading(isLoading: Boolean) {
        _accountState.update {
            it.mapSuccess {
                it.copy(
                    errors = it.errors.map {
                        if (it is ConditionErrorType.EmailVerification) {
                            it.copy(isLoading = isLoading)
                        } else {
                            it
                        }
                    }
                )
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

    private fun onShowDialog(dialog: MainAlertDialog?) {
        _accountState.update { it.mapSuccess { it.copy(alertDialog = dialog) } }
    }

    fun onEvent(event: AccountScreenUiEvent) {
        when (event) {
            AccountScreenUiEvent.DeleteAccount -> onShowDialog(MainAlertDialog.DeleteAccount)
            AccountScreenUiEvent.Logout -> onShowDialog(MainAlertDialog.Logout)
            AccountScreenUiEvent.ToggleLocationBoost -> toggleBoost()
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

            AccountScreenUiEvent.TryToOpenAppReview -> {
                _accountState.update {
                    it.mapSuccess {
                        it.copy(isAppReviewExpanded = !it.isAppReviewExpanded)
                    }
                }
            }

            AccountScreenUiEvent.OpenAppReview -> {
                appPrefs.wasAppReviewOpened = true
                _eventChannel.trySend(AccountScreenEvent.OpenAppReview)
                _accountState.update {
                    it.mapSuccess {
                        it.copy(isAppReviewVisible = false)
                    }
                }
            }

            AccountScreenUiEvent.CloseAppReview -> {
                appPrefs.wasAppReviewOpened = true
                _accountState.update {
                    it.mapSuccess {
                        it.copy(isAppReviewVisible = false)
                    }
                }
            }

            AccountScreenUiEvent.TriggerAppUpdate -> {
                appUpdateManager.startUpdateProcess(data = remoteConfigRepository.getCachedConfigOnce())
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
                        valueFormatter.formatTimeOnly(
                            timeUTC = it.toLocalDateTime(TimeZone.UTC),
                            showSeconds = false
                        )
                    }
                )
            }
        }
    }
}