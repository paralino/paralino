package app.paralino.core.presentation.map

import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.paralinoid.PersonalCodeScreenCodeData
import app.paralino.core.provider.DeviceStatusData
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.personal_code.PersonalCodesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.DeeplinkType
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class GroupsInitEmptyStateManager(
    private val userRepo: UserRepository,
    private val personalCodesRepo: PersonalCodesRepository,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    val deviceApiProvider: IDeviceApiProvider,
    private val appPrefs: AppPrefs,
    private val accountStateMonitor: AppAccountStateMonitor,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val coroutineScope: CoroutineScope,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
) {
    private val _uiState = MutableStateFlow(
        GroupsInitEmptyScreenState(
            alertDialog = null,
            personalCode = null,
            errors = emptyList()
        )
    )

    fun getUiState(): Flow<GroupsInitEmptyScreenState> {
        return combine(
            userRepo.localUser,
            deviceApiProvider.deviceStatus,
            personalCodesRepo.activePersonalCodeState,
            groupLocalDataSource.allGroupsState
                .mapNotNull { it.data?.size }
                .distinctUntilChanged(),
            _uiState.map { it.alertDialog }.distinctUntilChanged()
        ) { user, deviceStatus, code, groupCount, alertDialog ->
            mapState(
                previousAlertDialog = alertDialog,
                user = user,
                deviceStatus = deviceStatus,
                code = code,
            )
        }
            .onEach { state -> _uiState.update { state } }
            .distinctUntilChanged()
            .catch {
                messageDispatcher.showSnackbar(it.errorString())
            }
            .flowOn(Dispatchers.IO)
    }

    private fun mapState(
        previousAlertDialog: MainAlertDialog?,
        user: Resource<UserMe?>,
        deviceStatus: DeviceStatusData,
        code: UserPersonalCode?,
    ): GroupsInitEmptyScreenState {
        val errors = buildList {
            if (!deviceStatus.networkStatus.isOnline) {
                add(ConditionErrorType.Network)
            }

            if (!deviceStatus.notificationStatus.isNotificationPermissionAllowed) {
                add(ConditionErrorType.PermissionNotifications)
            }

            user.data?.canVerifyEmail?.takeIf { it }?.let {
                add(
                    ConditionErrorType.EmailVerification(
                        email = user.data?.email ?: "",
                        isLoading = _uiState.value.errors
                            .filterIsInstance<ConditionErrorType.EmailVerification>()
                            .firstOrNull()
                            ?.isLoading
                            ?: false
                    )
                )
            }
        }

        val personalCode = PersonalCodeScreenCodeData.viewOnlyCode(code)

        return GroupsInitEmptyScreenState(
            personalCode = personalCode,
            errors = errors,
            alertDialog = previousAlertDialog
        )
    }

    fun onEvent(event: GroupsInitEmptyScreenUiEvent) {
        when (event) {
            GroupsInitEmptyScreenUiEvent.CopyPersonalCode -> {
                copyPersonalCodeToClipboard()
            }

            is GroupsInitEmptyScreenUiEvent.DialogAction -> {
                _uiState.update { it.copy(alertDialog = null) }
                when (event.action.dialog) {
                    is MainAlertDialog.VerifyEmail -> {
                        if (event.action.isOk) {
                            sendEmailVerification()
                        }
                    }
                    else -> {}
                }
            }

            is GroupsInitEmptyScreenUiEvent.ErrorItemAction -> {
                when (event.item) {
                    is ConditionErrorType.EmailVerification -> {
                        _uiState.update {
                            it.copy(
                                alertDialog = userRepo.cachedUser?.email?.let { MainAlertDialog.VerifyEmail(it) }
                            )
                        }
                    }

                    ConditionErrorType.Network -> {
                        messageDispatcher.showSnackbar(
                            text = MR.strings.error_no_connection_snackbar.localizedString()
                        )
                    }

                    ConditionErrorType.PermissionNotifications -> {
                        eventChannel.trySend(MapHomeScreenEvent.AskNotificationPermission)
                    }

                    else -> {
                        /* do nothing */
                    }
                }
            }

            GroupsInitEmptyScreenUiEvent.OpenPersonalCodes -> {
                eventChannel.trySend(MapHomeScreenEvent.OpenPersonalCodes)
            }

            is GroupsInitEmptyScreenUiEvent.SharePersonalCode -> {
                sharePersonalCode()
            }

            GroupsInitEmptyScreenUiEvent.TryCreateGroup -> {
                eventChannel.trySend(MapHomeScreenEvent.OpenGroupCreateScreen)
            }
        }
    }

    private fun sendEmailVerification() {
        val isEmailSendingLoading = _uiState.value.errors
            .filterIsInstance<ConditionErrorType.EmailVerification>()
            .firstOrNull()
            ?.isLoading
            ?: false

        if (isEmailSendingLoading) {
            return
        }

        coroutineScope.launch {
            try {
                appPrefs.emailVerificationSkipTime = null
                setEmailVerificationLoading(true)
                userRepo.initEmailVerification()
                messageDispatcher.showSnackbar(MR.strings.info_email_check_instructions_popup.localizedString())
                setEmailVerificationLoading(false)

                // a bit of delay for the ui
                delay(1.seconds)
                accountStateMonitor.triggerStateCheck()
            } catch (e: Exception) {
                setEmailVerificationLoading(false)
                messageDispatcher.showSnackbar(e.errorString())
                accountStateMonitor.triggerStateCheck()
            }
        }
    }

    private fun setEmailVerificationLoading(isLoading: Boolean) {
        _uiState.update {
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

    private fun copyPersonalCodeToClipboard() {
        deviceApiProvider.copyToClipboard(_uiState.value.personalCode?.code ?: "")
        messageDispatcher.showSnackbar(MR.strings.toast_copy_invite_code.localizedString())
    }

    private fun sharePersonalCode() {
        _uiState.value.personalCode?.code?.let {
            eventChannel.trySend(
                MapHomeScreenEvent.SharePersonalCode(DeeplinkType.ParalinoID.buildUrl(it))
            )
        }
    }
}