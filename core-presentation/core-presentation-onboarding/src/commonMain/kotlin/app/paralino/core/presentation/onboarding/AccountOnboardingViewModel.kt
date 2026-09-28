package app.paralino.core.presentation.onboarding

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.group.PendingGroupJoinWelcomeUseCase
import app.paralino.core.domain.notifications.PushNotificationHandler
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.data
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
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.DeeplinkType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.poll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AccountOnboardingViewModel(
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val notificationServiceManager: NotificationSocketServiceManager,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
    private val pendingGroupJoinWelcome: PendingGroupJoinWelcomeUseCase,
    private val pushNotificationHandler: PushNotificationHandler,
) : CoreViewModel() {

    private var waitingForGroupJob: Job? = null

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

        combine(
            uiState
                .map { it.data?.onboardingStep }
                .distinctUntilChanged(),
            pendingGroupJoinWelcome.observeFirstUnshownGroupId()
        ) { onboardingStep, groupId ->
            groupId?.takeIf {
                when (onboardingStep) {
                    AccountOnboardingContent.GroupChoice,
                    is AccountOnboardingContent.ParalinoID -> true

                    else -> false
                }
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { groupId -> openGroupJoinWelcome(groupId) }
            .catch { showSnackbar(it.errorString()) }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        uiState
            .map { (it.data?.onboardingStep as? AccountOnboardingContent.ParalinoID)?.didShareCode == true }
            .distinctUntilChanged()
            .onEach { isWaiting ->
                if (isWaiting) startWaitingForGroup() else stopWaitingForGroup()
            }
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
            val previousStep = previousState.onboardingStep
            if (previousStep is AccountOnboardingContent.ParalinoID) {
                previousStep.copy(personalCode = personalCode)
            } else {
                previousStep
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
                    AccountOnboardingContent.GroupOnboardingIntro
                }

                else -> {
                    markOnboardingFinished()
                    activatePushNotificationServiceIfNeeded()
                    null
                }
            }
        }

        val startsWithGroupOnboarding = previousState == null &&
                onboardingStep is AccountOnboardingContent.GroupOnboardingIntro

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
                    AccountOnboardingUiEvent.SelectJoinGroup -> openParalinoIDStep()
                    AccountOnboardingUiEvent.SelectCreateGroup -> openGroupCreateStep()
                }
            }
        }
    }

    private fun confirmOnboarding() {
        when (val step = uiState.value.data?.onboardingStep) {
            AccountOnboardingContent.GroupOnboardingIntro -> {
                openGroupChoiceStep()
            }

            AccountOnboardingContent.GroupChoice -> {
                // no-op, can't confirm this screen
            }

            AccountOnboardingContent.GroupCreate -> {
                openGroupCreate()
            }

            is AccountOnboardingContent.ParalinoID -> {
                if (step.didShareCode) {
                    // Final close button
                    finishOnboarding()
                } else if (uiState.value.data?.personalCode != null) {
                    sharePersonalCode()
                } else {
                    _eventChannel.trySend(AccountOnboardingScreenEvent.OpenPersonalCodes)
                }
            }

            AccountOnboardingContent.PushNotifications -> {
                _eventChannel.trySend(AccountOnboardingScreenEvent.AskNotificationPermission)
            }

            AccountOnboardingContent.PrivacyZonesIntro -> {
                finishOnboarding()
            }

            null -> {
            }
        }
    }

    private fun skipOnboarding() {
        when (uiState.value.data?.onboardingStep) {
            AccountOnboardingContent.GroupOnboardingIntro -> {
                // no-op, no skip button
            }

            AccountOnboardingContent.GroupChoice -> {
                // "I'll decide later"
                if (uiState.value.data?.didShareParalinoID == true) {
                    finishOnboarding()
                } else {
                    finishOnboardingOrGoToPayment()
                }
            }

            AccountOnboardingContent.GroupCreate -> {
                // "Someone already invited me"
                openParalinoIDStep()
            }

            is AccountOnboardingContent.ParalinoID -> {
                // "I'm starting a group instead"
                openGroupCreateStep()
            }

            AccountOnboardingContent.PushNotifications -> {
                startAccountStartOnboarding()
            }

            AccountOnboardingContent.PrivacyZonesIntro -> {
                finishOnboarding()
            }

            null -> {}
        }
    }

    private fun openGroupChoiceStep() {
        _uiState.update {
            it.mapReady {
                it.copy(onboardingStep = AccountOnboardingContent.GroupChoice)
            }
        }
    }

    private fun openParalinoIDStep() {
        _uiState.update {
            it.mapReady {
                it.copy(
                    onboardingStep = AccountOnboardingContent.ParalinoID(
                        personalCode = it.personalCode,
                        didShareCode = it.didShareParalinoID,
                        canCloseWaiting = false
                    )
                )
            }
        }
    }

    private fun openGroupCreateStep() {
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
        stopWaitingForGroup()
        _uiState.update { it.mapReady { it.copy(onboardingStep = null) } }
    }

    private fun startAccountStartOnboarding() {
        if (appPrefs.didHaveAccountWelcomeOnboarding) {
            finishOnboarding()
        } else if (uiState.value.data?.groupCount == 0) {
            _uiState.update {
                it.mapReady {
                    it.copy(
                        isGroupModeChecked = true,
                        onboardingStep = AccountOnboardingContent.GroupOnboardingIntro
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

    private fun startWaitingForGroup() {
        waitingForGroupJob?.cancel()
        waitingForGroupJob = coroutineScope.launch {
            launch {
                delay(20.seconds)
                _uiState.update {
                    it.mapReady { state ->
                        val step = state.onboardingStep as? AccountOnboardingContent.ParalinoID
                        if (step != null && step.didShareCode) {
                            state.copy(onboardingStep = step.copy(canCloseWaiting = true))
                        } else {
                            state
                        }
                    }
                }
            }

            try {
                poll(
                    firstDelayMillis = 10.seconds.inWholeMilliseconds,
                    delay = 10.seconds.inWholeMilliseconds,
                    timeout = 3.minutes.inWholeMilliseconds
                ) {
                    pushNotificationHandler.checkForPushNotifications()
                    pendingGroupJoinWelcome.observeFirstUnshownGroupId().first()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun stopWaitingForGroup() {
        waitingForGroupJob?.cancel()
        waitingForGroupJob = null
    }

    private suspend fun openGroupJoinWelcome(groupId: String) {
        pendingGroupJoinWelcome.markShown(groupId = groupId)
        _eventChannel.trySend(AccountOnboardingScreenEvent.OpenGroupJoinWelcome(groupId = groupId))
        delay(600)
        finishOnboarding()
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
