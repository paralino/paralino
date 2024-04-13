package app.paralino.core.presentation.groups

import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupInitViewModel(
    private val connectionStatus: IConnectionStatusProvider,
    val locationStatusProvider: ILocationStatusProvider,
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val deviceApiProvider: IDeviceApiProvider
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(GroupInitScreenState())
    val uiState: Flow<GroupInitScreenState> = _uiState

    private val _eventChannel = Channel<GroupInitScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            userRepo.localUser,
            connectionStatus.status,
            locationStatusProvider.status,
            personalCodesRepo.getAndRefreshMyPersonalCodes()
        ) { user, network, locationStatus, codes ->

            val errors = buildList {
                if (!network.isOnline)
                    add(ConditionErrorType.NETWORK)

                if (!locationStatus.isNotificationPermissionAllowed)
                    add(ConditionErrorType.PERMISSION_NOTIFICATIONS)

                user.data?.needsEmailVerification?.takeIf { it }?.let {
                    add(ConditionErrorType.EMAIL_VERIFICATION(user.data?.email ?: ""))
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

            GroupInitScreenState(
                userEmail = user.data?.email,
                userImage = user.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot),
                personalCode = personalCode,
                errors = errors
            )
        }
            .onEach { result ->
                _uiState.update { result }
            }
            .catch {
                showSnackbar(it.errorString())
            }
            .launchIn(coroutineScope)
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