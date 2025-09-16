package app.paralino.core.presentation.groups.edit

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.Resource
import app.paralino.core.models.Resource.NotInitialized.mapReady
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.ICoreMessageDispatcherEvent
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.NotificationPermissionResultEvent
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.ui.onboarding.account.AccountOnboardingContent
import app.paralino.core.ui.onboarding.account.AccountOnboardingUiEvent
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.utils.ConditionErrorType
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

class GroupInitViewModel(
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Resource<GroupInitScreenState>>(Resource.NotInitialized)
    val uiState: StateFlow<Resource<GroupInitScreenState>> = _uiState

    private val _eventChannel = Channel<GroupInitScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val canDisplayMap: Flow<Resource<Boolean>> = uiState
        .map { state ->
            if (state is Resource.Ready) {
                Resource.Ready(
                    data = state.data.onboardingStep == null
                )
            } else {
                Resource.NotInitialized
            }
        }

    val shouldShowFreePlanNoticeScreen: Boolean
        get() = userRepo.shouldShowFreePlanNoticeScreen

    private val shouldProceedToPaywall: Boolean
        get() {
            return !userRepo.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
        }

    init {
        combine(
            userRepo.localUser,
            deviceApiProvider.deviceStatus,
            personalCodesRepo.personalCodeLocalState,
            groupLocalDataSource.allGroupsState
                .mapNotNull { it.data?.size }
                .distinctUntilChanged()
        ) { user, deviceStatus, codes, groupCount ->
            _uiState.update {
                Resource.Ready(
                    mapState(
                        previousState = it.data,
                        user = user,
                        deviceStatus = deviceStatus,
                        codes = codes,
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
        previousState: GroupInitScreenState?,
        user: Resource<UserMe?>,
        deviceStatus: DeviceStatusData,
        codes: List<UserPersonalCode>,
        groupCount: Int
    ): GroupInitScreenState {
        val errors = buildList {
            if (!deviceStatus.networkStatus.isOnline) {
                add(ConditionErrorType.Network)
            }

            if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                add(ConditionErrorType.PermissionNotifications)
            }

            user.data?.canVerifyEmail?.takeIf { it }?.let {
                add(ConditionErrorType.EmailVerification(user.data?.email ?: ""))
            }
        }

        val code = codes.firstOrNull { it.isStillValid() }
        val personalCode = code?.let {
            PersonalCodeScreenCodeData(
                id = code.id,
                code = it.formattedCode,
                userImage = it.codeData.data?.ownerImage ?: ImageModel.emoji(Emojis.robot),
                userName = it.codeData.data?.ownerName ?: "---",
                canDisable = false,
                isValid = true,
                isActionLoading = false
            )
        }

        return GroupInitScreenState(
            userImage = user.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
            personalCode = personalCode,
            errors = errors,
            alertDialog = previousState?.alertDialog,
            onboardingStep = if (previousState != null) {
                if (previousState.onboardingStep is AccountOnboardingContent.ParalinoID) {
                    previousState.onboardingStep.copy(personalCode = personalCode)
                } else {
                    previousState.onboardingStep
                }
            } else {
                if (appPrefs.didHaveAccountWelcomeOnboarding) {
                    null
                } else if (groupCount == 0) {
                    AccountOnboardingContent.ParalinoID(personalCode = personalCode, didShareCode = false)
                } else if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed && !appPrefs.didAskForNotificationPermissions) {
                    appPrefs.didAskForNotificationPermissions = true
                    AccountOnboardingContent.PushNotifications
                } else {
                    null
                }
            },
            groupCount = groupCount
        )
    }

    fun onEvent(event: GroupInitScreenUiEvent) {
        when (event) {
            GroupInitScreenUiEvent.ConfirmSendEmailVerification -> {
                sendEmailVerification()
            }

            GroupInitScreenUiEvent.CopyPersonalCode -> {
                copyPersonalCodeToClipboard()
            }

            GroupInitScreenUiEvent.HideDialog -> {
                _uiState.update { it.mapReady { it.copy(alertDialog = null) } }
            }

            GroupInitScreenUiEvent.SendEmailVerification -> {
                _uiState.update {
                    it.mapReady {
                        it.copy(alertDialog = userRepo.cachedUser?.email?.let { MainAlertDialog.VerifyEmail(it) })
                    }
                }
            }

            is GroupInitScreenUiEvent.OnboardingStep -> {
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
                if (shouldShowFreePlanNoticeScreen) {
                    _uiState.update {
                        it.mapReady {
                            it.copy(
                                onboardingStep = AccountOnboardingContent.SilverPlan(
                                    userRepo.userPlans.value.highestValidPlanConfig
                                )
                            )
                        }
                    }
                } else {
                    openGroupCreate()
                }
            }

            is AccountOnboardingContent.ParalinoID -> {
                if (step.didShareCode) {
                    skipParalinoIDOnboarding()
                } else {
                    confirmParalinoIDOnboarding()
                }
            }

            AccountOnboardingContent.PushNotifications -> {
                _eventChannel.trySend(GroupInitScreenEvent.AskNotificationPermission)
            }

            is AccountOnboardingContent.SilverPlan -> {
                appPrefs.didStartedGroupOnboarding = true
                openGroupCreate()
            }

            null -> {}
        }
    }

    private fun skipOnboarding() {
        when (val step = uiState.value.data?.onboardingStep) {
            AccountOnboardingContent.GroupCreate -> {
                if (shouldProceedToPaywall) {
                    _eventChannel.trySend(GroupInitScreenEvent.OpenPaywall(type = BillingScreenSourceType.SKIPPED_ONBOARDING))
                    coroutineScope.launch {
                        delay(1000)
                        finishOnboarding()
                    }
                } else {
                    finishOnboarding()
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
                startGroupCreateOnboarding()
            }

            is AccountOnboardingContent.SilverPlan -> {
                finishOnboarding()
            }

            null -> {}
        }
    }

    private fun confirmParalinoIDOnboarding() {
        if (uiState.value.data?.personalCode != null) {
            sharePersonalCode()
        } else {
            _eventChannel.trySend(GroupInitScreenEvent.OpenPersonalCodes)
        }
    }

    private fun skipParalinoIDOnboarding() {
        if (deviceApiProvider.notificationStatusProvider.currentStatus.isNotificationPermissionAllowed) {
            startGroupCreateOnboarding()
        } else {
            _uiState.update {
                appPrefs.didAskForNotificationPermissions = true
                it.mapReady {
                    it.copy(onboardingStep = AccountOnboardingContent.PushNotifications)
                }
            }
        }
    }

    private fun finishOnboarding() {
        appPrefs.didHaveAccountWelcomeOnboarding = true
        _uiState.update { it.mapReady { it.copy(onboardingStep = null) } }
    }
    
    private fun startGroupCreateOnboarding() {
        if (uiState.value.data?.groupCount == 0) {
            _uiState.update {
                it.mapReady {
                    it.copy(onboardingStep = AccountOnboardingContent.GroupCreate)
                }
            }
        } else {
            finishOnboarding()
        }
    }

    private fun openGroupCreate() {
        _eventChannel.trySend(GroupInitScreenEvent.OpenGroupCreate)
        coroutineScope.launch {
            delay(600)
            finishOnboarding()
        }
    }

    private fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                showSnackbar(MR.strings.info_email_check_instructions_popup.localizedString())
            } catch (e: Exception) {
                showSnackbar(e.errorString())
            }
        }
    }

    private fun copyPersonalCodeToClipboard() {
        deviceApiProvider.copyToClipboard(_uiState.value.data?.personalCode?.code ?: "")
        showSnackbar(MR.strings.toast_copy_invite_code.localizedString())
    }

    private fun sharePersonalCode() {
        uiState.value.data?.personalCode?.code?.let {
            _eventChannel.trySend(
                GroupInitScreenEvent.SharePersonalCode(DeeplinkType.ParalinoID.buildUrl(it))
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
            onEvent(GroupInitScreenUiEvent.OnboardingStep(AccountOnboardingUiEvent.SkipOnboarding))
            return true
        }

        return false
    }
}