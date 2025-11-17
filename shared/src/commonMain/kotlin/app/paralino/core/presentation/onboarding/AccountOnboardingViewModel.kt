package app.paralino.core.presentation.onboarding

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.ICoreMessageDispatcherEvent
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.presentation.onboarding.AccountOnboardingScreenEvent.OpenPaywall
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationPermissionResultEvent
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.onboarding.account.AccountOnboardingContent
import app.paralino.core.ui.onboarding.account.AccountOnboardingUiEvent
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
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
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
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
            return !userRepo.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
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
                    if (!appPrefs.didHaveLocationHistoryIntroOnboarding) {
                        AccountOnboardingContent.PersonalSpaceCurrentUsersIntro
                    } else {
                        null
                    }
                }

                !deviceStatus.notificationStatus.isNotificationPermissionAllowed -> {
                    appPrefs.didAskForNotificationPermissions = true
                    AccountOnboardingContent.PushNotifications
                }

                groupCount == 0 -> {
                    AccountOnboardingContent.AccountOnboardingStart(
                        isGroupChecked = previousState?.isGroupModeChecked ?: false,
                        isPersonalSpaceChecked = previousState?.isPersonalSpaceModeChecked ?: false
                    )
                }

                else -> {
                    AccountOnboardingContent.PersonalSpaceNewUsersIntro
                }
            }
        }

        return AccountOnboardingScreenState(
            userImage = user.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
            personalCode = personalCode,
            onboardingStep = onboardingStep,
            groupCount = groupCount,
            fullScreenLoading = previousState?.fullScreenLoading ?: false,
            isGroupModeChecked = previousState?.isGroupModeChecked ?: false,
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

                    AccountOnboardingUiEvent.GoWithGroupMode -> {
                        _uiState.update {
                            it.mapReady {
                                it.copy(
                                    onboardingStep = AccountOnboardingContent.ParalinoID(
                                        personalCode = it.personalCode,
                                        didShareCode = false
                                    ),
                                    isGroupModeChecked = true
                                )
                            }
                        }
                    }

                    AccountOnboardingUiEvent.GoWithPersonalSpaceMode -> {
                        _uiState.update {
                            it.mapReady {
                                it.copy(
                                    onboardingStep = AccountOnboardingContent.PersonalSpaceNewUsersIntro,
                                    isPersonalSpaceModeChecked = true
                                )
                            }
                        }
                    }
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

            AccountOnboardingContent.PersonalSpaceCurrentUsersIntro -> {
                confirmPersonalSpaceIntro()
            }

            is AccountOnboardingContent.LocationHistoryIntro -> {
                confirmLocationHistoryInit(step)
            }

            AccountOnboardingContent.PersonalSpaceNewUsersIntro -> {
                openPersonalSpaceSetup()
            }

            is AccountOnboardingContent.AccountOnboardingStart -> {
                skipAccountStartOnboarding()
            }

            null -> {
            }
        }
    }

    private fun skipOnboarding() {
        when (val step = uiState.value.data?.onboardingStep) {
            AccountOnboardingContent.GroupCreate -> {
                // go back
                startAccountStartOnboarding()
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

            AccountOnboardingContent.PersonalSpaceCurrentUsersIntro -> {
                confirmPersonalSpaceIntro()
            }

            is AccountOnboardingContent.LocationHistoryIntro -> {
                finishOnboarding()
            }

            is AccountOnboardingContent.AccountOnboardingStart -> {
                skipAccountStartOnboarding()
            }

            AccountOnboardingContent.PersonalSpaceNewUsersIntro -> {
                val groupCount = uiState.value.data?.groupCount ?: 0
                if (groupCount > 0) {
                    finishOnboarding()
                } else {
                    // go back
                    startAccountStartOnboarding()
                }
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
            finishOnboarding()
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
            finishOnboarding()
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

    private fun skipAccountStartOnboarding() {
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
    }

    private fun finishOnboarding() {
        markOnboardingFinished()
        _uiState.update { it.mapReady { it.copy(onboardingStep = null) } }
    }

    private fun startAccountStartOnboarding() {
        if (appPrefs.didHaveAccountWelcomeOnboarding) {
            finishOnboarding()
        } else if (uiState.value.data?.groupCount == 0) {
            _uiState.update {
                it.mapReady {
                    it.copy(
                        onboardingStep = AccountOnboardingContent.AccountOnboardingStart(
                            isGroupChecked = it.isGroupModeChecked,
                            isPersonalSpaceChecked = it.isPersonalSpaceModeChecked
                        )
                    )
                }
            }
        } else if (_uiState.value.data?.isGroupModeChecked != true || _uiState.value.data?.isPersonalSpaceModeChecked != true) {
            _uiState.update {
                it.mapReady {
                    it.copy(
                        isGroupModeChecked = true,
                        onboardingStep = AccountOnboardingContent.PersonalSpaceNewUsersIntro
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
                        it.mapReady { it.copy(onboardingStep = newStep) }
                    }
                }
        }
    }

    override fun onMessageEvent(event: ICoreMessageDispatcherEvent): Boolean {
        if (event is NotificationPermissionResultEvent && uiState.value.data?.onboardingStep is AccountOnboardingContent.PushNotifications) {
            onEvent(AccountOnboardingScreenUiEvent.OnboardingStep(AccountOnboardingUiEvent.SkipOnboarding))
            return true
        }

        return false
    }
}