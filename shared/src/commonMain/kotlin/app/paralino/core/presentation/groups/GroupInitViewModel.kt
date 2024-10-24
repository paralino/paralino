package app.paralino.core.presentation.groups

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupInitViewModel(
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        mapState(
            user = userRepo.localUser.value,
            deviceStatus = deviceApiProvider.currentDeviceStatus(),
            codes = personalCodesRepo.personalCodeLocalState.value
        )
    )
    val uiState: StateFlow<GroupInitScreenState> = _uiState

    private val _eventChannel = Channel<GroupInitScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    val shouldShowFreePlanNoticeScreen: Boolean get() {
        return !userRepo.userPlans.value.highestValidPlanConfig.planTypeEnum.isPremium
                && !appPrefs.didStartedGroupOnboarding
    }

    init {
        coroutineScope.launch {
            try {
                personalCodesRepo.refreshPersonalCodesAndRestockKeys()
            } catch (e: Exception) {
            }
        }

        combine(
            userRepo.localUser,
            deviceApiProvider.deviceStatus,
            personalCodesRepo.personalCodeLocalState
        ) { user, deviceStatus, codes ->
            _uiState.update {
                mapState(user, deviceStatus, codes)
            }
        }
            .catch {
                showSnackbar(it.errorString())
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun mapState(
        user: Resource<UserMe?>,
        deviceStatus: DeviceStatusData,
        codes: List<UserPersonalCode>
    ): GroupInitScreenState {
        val errors = buildList {
            if (!deviceStatus.networkStatus.isOnline) {
                add(ConditionErrorType.Network)
            }

            if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                add(ConditionErrorType.PermissionNotifications)
            }

            user.data?.needsEmailVerification?.takeIf { it }?.let {
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
            userEmail = user.data?.email,
            userImage = user.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
            personalCode = personalCode,
            errors = errors
        )
    }

    private fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                userRepo.initEmailVerification()
                showSnackbar(MR.strings.info_email_check_instructions.localizedString())
            } catch (e: Exception) {
                showSnackbar(e.errorString())
            }
        }
    }

    private fun copyToClipboard() {
        deviceApiProvider.copyToClipboard(_uiState.value.personalCode?.code ?: "")
    }

    fun onEvent(event: GroupInitScreenUiEvent) {
        when (event) {
            GroupInitScreenUiEvent.ConfirmSendEmailVerification -> {
                sendEmailVerification()
            }

            GroupInitScreenUiEvent.CopyPersonalCode -> {
                copyToClipboard()
            }

            GroupInitScreenUiEvent.HideDialog -> {
                _uiState.update { it.copy(alertDialog = null) }
            }

            GroupInitScreenUiEvent.SendEmailVerification -> {
                _uiState.update {
                    it.copy(alertDialog = it.userEmail?.let { MainAlertDialog.VerifyEmail(it) })
                }
            }
        }
    }
}