package app.paralino.core.presentation.onboarding

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.onboarding.AccountOnboardingScreenEvent.OpenPaywall
import app.paralino.core.presentation.paralinoid.PersonalCodeScreenCodeData
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.ICoreMessageDispatcherEvent
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationPermissionResultEvent
import app.paralino.core.provider.NotificationSocketServiceManager
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.DeeplinkType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class AccountOnboardingViewModel(
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val locationSharingManager: LocationSharingManager,
    private val registerDevice: RegisterDeviceForSharingUseCase,
    private val notificationServiceManager: NotificationSocketServiceManager,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
    private val remoteConfigRepository: RemoteConfigRepository,
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow<Resource<AccountOnboardingScreenState>>(Resource.NotInitialized)
    val uiState: StateFlow<Resource<AccountOnboardingScreenState>> = _uiState

    private val _eventChannel = Channel<AccountOnboardingScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val canDisplayMap: Flow<Resource<Boolean>> = uiState
        .map { state ->
            state.mapReady { it.canDisplayMap }
        }

    private val shouldProceedToPaywall: Boolean
        get() {
            return !userRepo.userPlans.value.highestValidPlanConfig.isPremium
        }

    init {
        combine(
            userRepo.localUser,
            deviceApiProvider.deviceStatus,
            personalCodesRepo.activePersonalCodeState,
            groupLocalDataSource.allGroupsState
                .mapNotNull { it.data?.size }
                .distinctUntilChanged()
        ) { user, deviceStatus, code, groupCount ->
            _uiState.update {
                Resource.Ready(
                    mapState(
                        previousState = it.data,
                        user = user,
                        deviceStatus = deviceStatus,
                        code = code,
                        groupCount = groupCount
                    )
                )
            }
        }
            .catch {
                showSnackbar(it.errorString())
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        canDisplayMap
            .distinctUntilChanged()
            .onEach {
                if (it.data == true) {
                    if (appPrefs.detectedAppReadyTime == null) {
                        appPrefs.detectedAppReadyTime = nowUTC()
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        coroutineScope.launch {
            try {
                personalCodesRepo.refreshPersonalCodesAndRestockKeys(cacheDelayMillis = 2.minutes)
            } catch (e: Exception) {
            }
        }
    }

    private fun mapState(
        previousState: AccountOnboardingScreenState?,
        user: Resource<UserMe?>,
        deviceStatus: DeviceStatusData,
        code: UserPersonalCode?,
        groupCount: Int
    ): AccountOnboardingScreenState {

        val personalCode = PersonalCodeScreenCodeData.viewOnlyCode(code)

        val onboardingStep = if (previousState != null) {
            if (previousState.onboardingStep is AccountOnboardingContent.ParalinoID) {
                previousState.onboardingStep.copy(personalCode = personalCode)
            } else {
                previousState.onboardingStep
            }
        } else {
            when {
                appPrefs.didHaveAccountWelcomeOnboarding -> {
                    when {
                        !appPrefs.didHavePrivacyZonesIntroOnboarding -> {
                            AccountOnboardingContent.PrivacyZonesIntro
                        }

                        else -> null
                    }
                }

                !deviceStatus.notificationStatus.isNotificationPermissionAllowed -> {
                    appPrefs.didAskForNotificationPermissions = true
                    AccountOnboardingContent.PushNotifications
                }

                groupCount == 0 -> {
                    AccountOnboardingContent.ParalinoID(
                        personalCode = personalCode,
                        didShareCode = false
                    )
                }

                else -> {
                    markOnboardingFinished()
                    activatePushNotificationServiceIfNeeded()
                    null
                }
            }
        }

        val startsWithGroupOnboarding = previousState == null &&
            onboardingStep is AccountOnboardingContent.ParalinoID

        return AccountOnboardingScreenState(
            userImage = user.data?.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
            personalCode = personalCode,
            onboardingStep = onboardingStep,
            groupCount = groupCount,
            fullScreenLoading = previousState?.fullScreenLoading ?: false,
            isGroupModeChecked = previousState?.isGroupModeChecked ?: startsWithGroupOnboarding,
            didShareParalinoID = previousState?.didShareParalinoID ?: false,
            isPersonalSpaceModeChecked = previousState?.isPersonalSpaceModeChecked ?: false,
        )
    }

    fun onEvent(event: AccountOnboardingScreenUiEvent) {
        when (event) {
            is AccountOnboardingScreenUiEvent.OnboardingStep -> {
                when (event.event) {
                    AccountOnboardingUiEvent.ConfirmOnboarding -> confirmOnboarding()
                    AccountOnboardingUiEvent.SkipOnboarding -> skipOnboarding()
                    AccountOnboardingUiEvent.CopyPersonalCode -> copyPersonalCodeToClipboard()
                    AccountOnboardingUiEvent.SharePersonalCode -> sharePersonalCode()
                }
            }
        }
    }

    private fun confirmOnboarding() {
        when (val step = uiState.value.data?.onboardingStep) {
            AccountOnboardingContent.GroupCreate -> {
                openGroupCreate()
            }

            is AccountOnboardingContent.ParalinoID -> {
                if (step.didShareCode) {
                    skipParalinoIDOnboarding()
                } else {
                    confirmParalinoIDOnboarding()
                }
            }

            AccountOnboardingContent.PushNotifications -> {
                _eventChannel.trySend(AccountOnboardingScreenEvent.AskNotificationPermission)
            }

            is AccountOnboardingContent.LocationHistoryIntro -> {
                confirmLocationHistoryInit(step)
            }

            AccountOnboardingContent.PrivacyZonesIntro -> {
                finishOnboarding()
            }

            null -> {
            }
        }
    }

    private fun skipOnboarding() {
        when (val step = uiState.value.data?.onboardingStep) {
            AccountOnboardingContent.GroupCreate -> {
                if (uiState.value.data?.didShareParalinoID == true) {
                    finishOnboarding()
                } else {
                    finishOnboardingOrGoToPayment()
                }
            }

            is AccountOnboardingContent.ParalinoID -> {
                if (step.didShareCode) {
                    confirmParalinoIDOnboarding()
                } else {
                    skipParalinoIDOnboarding()
                }
            }

            AccountOnboardingContent.PushNotifications -> {
                startAccountStartOnboarding()
            }

            is AccountOnboardingContent.LocationHistoryIntro -> {
                openPrivacyZonesIntro()
            }

            AccountOnboardingContent.PrivacyZonesIntro -> {
                finishOnboarding()
            }

            null -> {}
        }
    }

    private fun confirmPersonalSpaceIntro() {
        // only showing history intro to current users with groups
        // if there are no groups, history onboarding should not be shown ever
        // since if user creates or joins a groups, they will see history elsewhere
        val groupCount = uiState.value.data?.groupCount ?: 0
        _uiState.update {
            it.mapReady {
                it.copy(
                    onboardingStep = AccountOnboardingContent.LocationHistoryIntro(
                        isSharingActive = if (groupCount > 0) {
                            locationSharingManager.isGroupSharingActiveMaybe()
                        } else {
                            false
                        }
                    )
                )
            }
        }
    }

    private fun confirmLocationHistoryInit(step: AccountOnboardingContent.LocationHistoryIntro) {
        if (registerDevice.isProcessingAny() || uiState.value.data?.fullScreenLoading == true) {
            return
        }

        if (!step.isSharingActive) {
            openPrivacyZonesIntro()
            return
        }

        _uiState.update { it.mapReady { it.copy(fullScreenLoading = true) } }
        coroutineScope.launch {
            val contracts = locationSharingManager.allValidContracts()
                .filter { it.groupOrSpaceId.isGroup }
                .map { it.copy(saveToHistory = true) }

            contracts.forEach { contract ->
                try {
                    registerDevice.registerDevice(
                        groupId = contract.groupOrSpaceId,
                        contract = contract,
                        deviceName = null,
                        notifyOthers = false,
                        isRetry = false
                    )
                    locationSharingManager.startSharing(contract)
                } catch (e: Exception) {
                    showSnackbar(e.errorString())
                }
            }
            _uiState.update { it.mapReady { it.copy(fullScreenLoading = false) } }
            openPrivacyZonesIntro()
        }
    }

    private fun openPrivacyZonesIntro() {
        _uiState.update {
            it.mapReady {
                it.copy(onboardingStep = AccountOnboardingContent.PrivacyZonesIntro)
            }
        }
    }

    private fun confirmParalinoIDOnboarding() {
        if (uiState.value.data?.personalCode != null) {
            sharePersonalCode()
        } else {
            _eventChannel.trySend(AccountOnboardingScreenEvent.OpenPersonalCodes)
        }
    }

    private fun skipParalinoIDOnboarding() {
        _uiState.update {
            it.mapReady {
                it.copy(onboardingStep = AccountOnboardingContent.GroupCreate)
            }
        }
    }

    private fun finishOnboardingOrGoToPayment() {
        if (shouldProceedToPaywall) {
            _eventChannel.trySend(OpenPaywall(type = BillingScreenSourceType.SKIPPED_ONBOARDING))
            coroutineScope.launch {
                delay(1000)
                finishOnboarding()
            }
        } else {
            finishOnboarding()
        }
    }

    private fun markOnboardingFinished() {
        appPrefs.didHaveAccountWelcomeOnboarding = true
        appPrefs.didHaveLocationHistoryIntroOnboarding = true
        appPrefs.didHavePrivacyZonesIntroOnboarding = true
    }

    private fun finishOnboarding() {
        markOnboardingFinished()
        activatePushNotificationServiceIfNeeded()
        _uiState.update { it.mapReady { it.copy(onboardingStep = null) } }
    }

    private fun startAccountStartOnboarding() {
        if (appPrefs.didHaveAccountWelcomeOnboarding) {
            finishOnboarding()
        } else if (uiState.value.data?.groupCount == 0) {
//            _uiState.update {
//                it.mapReady {
//                    it.copy(
//                        onboardingStep = AccountOnboardingContent.AccountOnboardingStart(
//                            isGroupChecked = it.isGroupModeChecked,
//                            isPersonalSpaceChecked = it.isPersonalSpaceModeChecked
//                        )
//                    )
//                }
//            }

            _uiState.update {
                it.mapReady {
                    it.copy(
                        isGroupModeChecked = true,
                        onboardingStep = AccountOnboardingContent.ParalinoID(
                            personalCode = it.personalCode,
                            didShareCode = false
                        )
                    )
                }
            }
        } else {
            finishOnboarding()
        }
    }

    private fun openGroupCreate() {
        _eventChannel.trySend(AccountOnboardingScreenEvent.OpenGroupCreate)
        appPrefs.isPersonalSpaceSelected = false
        coroutineScope.launch {
            delay(600)
            finishOnboarding()
        }
    }

    private fun openPersonalSpaceSetup() {
        _eventChannel.trySend(
            AccountOnboardingScreenEvent.OpenSharingStart(
                groupId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
            )
        )
        appPrefs.isPersonalSpaceSelected = true
        coroutineScope.launch {
            delay(600)
            finishOnboarding()
        }
    }

    private fun copyPersonalCodeToClipboard() {
        deviceApiProvider.copyToClipboard(_uiState.value.data?.personalCode?.code ?: "")
        showSnackbar(MR.strings.toast_copy_invite_code.localizedString())
    }

    private fun sharePersonalCode() {
        uiState.value.data?.personalCode?.code?.let {
            _eventChannel.trySend(
                AccountOnboardingScreenEvent.SharePersonalCode(DeeplinkType.ParalinoID.buildUrl(it))
            )
        }

        coroutineScope.launch {
            delay(1000)
            uiState.value.data
                ?.onboardingStep
                ?.let { it as? AccountOnboardingContent.ParalinoID }
                ?.copy(didShareCode = true)
                ?.let { newStep ->
                    _uiState.update {
                        it.mapReady {
                            it.copy(onboardingStep = newStep, didShareParalinoID = true)
                        }
                    }
                }
        }
    }

    private fun activatePushNotificationServiceIfNeeded() {
        if (deviceApiProvider.notificationStatusProvider.currentStatus.isNotificationPermissionAllowed) {
            notificationServiceManager.setNotificationPushProvider(provider = PushProvider.default)
        }
    }

    override fun onMessageEvent(event: ICoreMessageDispatcherEvent): Boolean {
        if (event is NotificationPermissionResultEvent && uiState.value.data?.onboardingStep is AccountOnboardingContent.PushNotifications) {
            activatePushNotificationServiceIfNeeded()
            onEvent(AccountOnboardingScreenUiEvent.OnboardingStep(AccountOnboardingUiEvent.SkipOnboarding))
            return true
        }

        return false
    }
}