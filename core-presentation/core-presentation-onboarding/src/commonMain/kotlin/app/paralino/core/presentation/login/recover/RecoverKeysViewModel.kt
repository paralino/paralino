package app.paralino.core.presentation.login.recover

import app.paralino.core.MR
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.settings.FormValidation
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class RecoverKeysViewModel(
    private val logoutUseCase: LogoutUseCase,
    private val recoverAccountUseCase: RecoverAccountUseCase,
    private val appAccountStateMonitor: AppAccountStateMonitor,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<RecoverKeysScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<RecoverKeysScreenState>> = _uiState

    private val _eventChannel = Channel<RecoverKeysScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private var keys = listOf<UserCryptoKeyRaw>()

    init {
        doFetchUserKeys()
    }

    private fun doFetchUserKeys() {
        flow {
            emit(recoverAccountUseCase.getUserKeysFromRemote())
        }
            .mapToPayload { it }
            .onEach { result ->
                if (result is Payload.Error) {
                    showSnackbar(result.exception.errorString())
                }

                if (result is Payload.Success) {
                    keys = result.data
                }

                val uiStep = when (result) {
                    is Payload.Success -> getInitialUiStep(userKeys = result.data)
                    is Payload.Error -> RecoverKeysUiStep.FetchKeysFailed
                    Payload.Loading -> null
                }

                _uiState.update { payload ->
                    val state = payload.data
                    when {
                        state == null -> {
                            uiStep?.let { Payload.Success(RecoverKeysScreenState(uiStep = it)) } ?: payload
                        }

                        else -> Payload.Success(
                            state.copy(
                                uiStep = uiStep ?: state.uiStep,
                                actionLoading = result is Payload.Loading
                            )
                        )
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun getInitialUiStep(userKeys: List<UserCryptoKeyRaw>): RecoverKeysUiStep {
        return when {
            userKeys.isEmpty() -> {
                if (appPrefs.hasGuestAccount()) {
                    RecoverKeysUiStep.MissingKeysResetOnlyGuest
                } else {
                    RecoverKeysUiStep.ResetClassicAccount(canRecoverOldKeys = false)
                }
            }

            else -> {
                if (appPrefs.hasGuestAccount()) {
                    RecoverKeysUiStep.TryRecoverGuestAccount
                } else {
                    RecoverKeysUiStep.TryRecoverClassicAccount()
                }
            }
        }
    }

    private fun doRecoverAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return

        val recoverOrResetFlow = when (validState.uiStep) {
            RecoverKeysUiStep.FetchKeysFailed -> null
            RecoverKeysUiStep.InvalidKeysResetGuestAccount -> null
            RecoverKeysUiStep.MissingKeysResetOnlyGuest -> null
            is RecoverKeysUiStep.ResetClassicAccount -> null
            is RecoverKeysUiStep.TryRecoverClassicAccount -> null
            RecoverKeysUiStep.TryRecoverGuestAccount -> null

            is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> {
                if (validateCurrentPassword() && validateOldPassword()) {
                    recoverAccountUseCase.recoverUserAccount(
                        oldPassword = validState.uiStep.confirmedOldPassword,
                        currentPassword = validState.uiStep.currentPassword,
                        key = validState.uiStep.keys
                    )
                } else {
                    null
                }
            }

            is RecoverKeysUiStep.RecoverGuestAccountFinalStep -> {
                recoverAccountUseCase.recoverGuestAccount(
                    key = validState.uiStep.keys
                )
            }
        }

        if (recoverOrResetFlow != null) {
            recoverOrResetFlow
                .mapToPayload { it }
                .onEach { result ->

                    if (result is Payload.Error) {
                        showSnackbar(result.exception.errorString())
                    }

                    _uiState.update {
                        it.mapSuccess {
                            when (val step = it.uiStep) {
                                is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> {
                                    it.copy(
                                        actionLoading = result is Payload.Loading,
                                        uiStep = when (result) {
                                            is Payload.Success -> step

                                            is Payload.Error -> {
                                                step.copy(
                                                    hasWrongCurrentPasswordError = result.exception is UnauthorizedException
                                                )
                                            }

                                            Payload.Loading -> {
                                                step.copy(
                                                    hasCurrentPasswordError = false,
                                                    hasWrongCurrentPasswordError = false
                                                )
                                            }
                                        }
                                    )
                                }

                                else -> it.copy(actionLoading = result is Payload.Loading)
                            }
                        }
                    }

                    if (result is Payload.Success) {
                        appAccountStateMonitor.triggerStateCheck()
                    }
                }
                .launchIn(coroutineScope)
        } else {
            showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
        }
    }

    private fun doResetAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return

        val resetFlow = when (validState.uiStep) {
            RecoverKeysUiStep.InvalidKeysResetGuestAccount,
            RecoverKeysUiStep.MissingKeysResetOnlyGuest -> {
                recoverAccountUseCase.resetGuestAccount()
            }

            is RecoverKeysUiStep.ResetClassicAccount -> {
                if (validateCurrentPassword()) {
                    recoverAccountUseCase.resetUserAccount(validState.uiStep.currentPassword)
                } else {
                    null
                }
            }

            RecoverKeysUiStep.FetchKeysFailed -> null
            is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> null
            is RecoverKeysUiStep.RecoverGuestAccountFinalStep -> null
            is RecoverKeysUiStep.TryRecoverClassicAccount -> null
            RecoverKeysUiStep.TryRecoverGuestAccount -> null
        }

        if (resetFlow != null) {
            resetFlow
                .mapToPayload { it }
                .onEach { result ->

                    if (result is Payload.Error) {
                        showSnackbar(result.exception.errorString())
                    }

                    _uiState.update {
                        it.mapSuccess {
                            when (val step = it.uiStep) {
                                is RecoverKeysUiStep.ResetClassicAccount -> {
                                    it.copy(
                                        actionLoading = result is Payload.Loading,
                                        uiStep = when (result) {
                                            is Payload.Success -> step

                                            is Payload.Error -> {
                                                step.copy(
                                                    hasWrongCurrentPasswordError = result.exception is UnauthorizedException
                                                )
                                            }

                                            Payload.Loading -> {
                                                step.copy(
                                                    hasCurrentPasswordError = false,
                                                    hasWrongCurrentPasswordError = false
                                                )
                                            }
                                        }
                                    )
                                }

                                else -> it.copy(actionLoading = result is Payload.Loading)
                            }
                        }
                    }
                }
                .flowOn(Dispatchers.IO)
                .launchIn(coroutineScope)
        } else {
            showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
        }
    }

    private fun doCheckForKeyState() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return

        val checkFlow: Flow<GeneratedUserKeys?>? = when (validState.uiStep) {
            RecoverKeysUiStep.FetchKeysFailed -> null
            RecoverKeysUiStep.InvalidKeysResetGuestAccount -> null
            RecoverKeysUiStep.MissingKeysResetOnlyGuest -> null
            is RecoverKeysUiStep.ResetClassicAccount -> null
            is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> null
            is RecoverKeysUiStep.RecoverGuestAccountFinalStep -> null

            is RecoverKeysUiStep.TryRecoverClassicAccount -> {
                if (validateOldPassword()) {
                    flow {
                        val key = recoverAccountUseCase.getCorrectUserKeyForPassword(
                            keys = keys,
                            password = validState.uiStep.oldPassword
                        )
                        emit(key)
                    }
                } else {
                    null
                }
            }

            RecoverKeysUiStep.TryRecoverGuestAccount -> {
                flow {
                    val key = recoverAccountUseCase.getCorrectGuestUserKey(keys = keys)
                    emit(key)
                }
            }
        }

        if (checkFlow != null) {
            checkFlow
                .mapToPayload { it }
                .onEach { result ->
                    if (result is Payload.Error) {
                        showSnackbar(result.exception.errorString())
                    }

                    _uiState.update {
                        it.mapSuccess { state ->
                            when (val step = state.uiStep) {
                                RecoverKeysUiStep.FetchKeysFailed,
                                RecoverKeysUiStep.InvalidKeysResetGuestAccount,
                                RecoverKeysUiStep.MissingKeysResetOnlyGuest,
                                is RecoverKeysUiStep.RecoverClassicAccountFinalStep,
                                is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
                                is RecoverKeysUiStep.ResetClassicAccount -> {
                                    state.copy(actionLoading = result is Payload.Loading)
                                }

                                is RecoverKeysUiStep.TryRecoverGuestAccount -> {
                                    state.copy(
                                        actionLoading = result is Payload.Loading,
                                        uiStep = when (result) {
                                            is Payload.Success -> {
                                                val keys = result.data
                                                if (keys != null) {
                                                    RecoverKeysUiStep.RecoverGuestAccountFinalStep(keys = keys)
                                                } else {
                                                    RecoverKeysUiStep.InvalidKeysResetGuestAccount
                                                }
                                            }

                                            else -> step
                                        }
                                    )
                                }

                                is RecoverKeysUiStep.TryRecoverClassicAccount -> {
                                    state.copy(
                                        actionLoading = result is Payload.Loading,
                                        uiStep = when (result) {
                                            is Payload.Success -> {
                                                val keys = result.data
                                                if (keys != null) {
                                                    RecoverKeysUiStep.RecoverClassicAccountFinalStep(
                                                        confirmedOldPassword = state.uiStep.oldPassword,
                                                        currentPassword = "",
                                                        hasCurrentPasswordError = false,
                                                        hasWrongCurrentPasswordError = false,
                                                        keys = keys
                                                    )
                                                } else {
                                                    state.uiStep.copy(
                                                        hasWrongOldPasswordError = true
                                                    )
                                                }
                                            }
                                            is Payload.Error<*> -> step.copy(hasWrongOldPasswordError = false)
                                            Payload.Loading -> step
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                .flowOn(Dispatchers.IO)
                .launchIn(coroutineScope)
        } else {
            showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
        }
    }

    private fun validateOldPassword(): Boolean {
        val password = when (val step = _uiState.value.data?.uiStep) {
            is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> step.confirmedOldPassword
            is RecoverKeysUiStep.TryRecoverClassicAccount -> step.oldPassword
            RecoverKeysUiStep.FetchKeysFailed,
            RecoverKeysUiStep.MissingKeysResetOnlyGuest,
            is RecoverKeysUiStep.ResetClassicAccount,
            is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
            RecoverKeysUiStep.TryRecoverGuestAccount,
            RecoverKeysUiStep.InvalidKeysResetGuestAccount,
            null -> return false
        }

        val passwordValidation = FormValidation.TextNotEmpty.validate(password)

        _uiState.update {
            it.mapSuccess { state ->
                when (val step = state.uiStep) {
                    is RecoverKeysUiStep.TryRecoverClassicAccount -> state.copy(
                        uiStep = step.copy(
                            hasOldPasswordError = !passwordValidation.result
                        )
                    )

                    is RecoverKeysUiStep.RecoverClassicAccountFinalStep,
                    RecoverKeysUiStep.FetchKeysFailed,
                    RecoverKeysUiStep.MissingKeysResetOnlyGuest,
                    is RecoverKeysUiStep.ResetClassicAccount,
                    is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
                    RecoverKeysUiStep.TryRecoverGuestAccount,
                    RecoverKeysUiStep.InvalidKeysResetGuestAccount -> state
                }
            }
        }

        if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordValidation.type))
        }

        return passwordValidation.result
    }

    private fun validateCurrentPassword(): Boolean {
        val password = when (val step = _uiState.value.data?.uiStep) {
            is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> step.currentPassword
            is RecoverKeysUiStep.ResetClassicAccount -> step.currentPassword
            is RecoverKeysUiStep.TryRecoverClassicAccount,
            RecoverKeysUiStep.FetchKeysFailed,
            RecoverKeysUiStep.MissingKeysResetOnlyGuest,
            is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
            RecoverKeysUiStep.TryRecoverGuestAccount,
            RecoverKeysUiStep.InvalidKeysResetGuestAccount,
            null -> return false
        }

        val passwordValidation = FormValidation.TextNotEmpty.validate(password)

        _uiState.update {
            it.mapSuccess { state ->
                when (val step = state.uiStep) {
                    is RecoverKeysUiStep.ResetClassicAccount -> state.copy(
                        uiStep = step.copy(
                            hasCurrentPasswordError = !passwordValidation.result
                        )
                    )

                    is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> state.copy(
                        uiStep = step.copy(
                            hasCurrentPasswordError = !passwordValidation.result
                        )
                    )

                    is RecoverKeysUiStep.TryRecoverClassicAccount,
                    RecoverKeysUiStep.FetchKeysFailed,
                    RecoverKeysUiStep.MissingKeysResetOnlyGuest,
                    is RecoverKeysUiStep.RecoverGuestAccountFinalStep,
                    RecoverKeysUiStep.TryRecoverGuestAccount,
                    RecoverKeysUiStep.InvalidKeysResetGuestAccount -> state
                }
            }
        }

        if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordValidation.type))
        }

        return passwordValidation.result
    }

    private fun logout() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validState.isInProgress) return
        flow {
            logoutUseCase.fullLogout(logoutLocallyOnError = false)
            emit(Unit)
        }
            .mapToPayload { Unit }
            .onEach { state ->
                state.exception?.let { showSnackbar(it.errorString()) }
                _uiState.update {
                    it.mapSuccess {
                        it.copy(logoutLoading = state is Payload.Loading)
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun onOldPasswordChange(password: String) {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validState.isInProgress) return
        _uiState.update {
            it.mapSuccess {
                if (it.uiStep is RecoverKeysUiStep.TryRecoverClassicAccount) {
                    it.copy(
                        uiStep = it.uiStep.copy(
                            oldPassword = password,
                            hasOldPasswordError = false,
                            hasWrongOldPasswordError = false
                        )
                    )
                } else {
                    it
                }
            }
        }
    }

    private fun onCurrentPasswordChange(password: String) {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validState.isInProgress) return
        _uiState.update {
            it.mapSuccess {
                when (it.uiStep) {
                    is RecoverKeysUiStep.ResetClassicAccount -> {
                        it.copy(
                            uiStep = it.uiStep.copy(
                                currentPassword = password,
                                hasCurrentPasswordError = false,
                                hasWrongCurrentPasswordError = false
                            )
                        )
                    }

                    is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> {
                        it.copy(
                            uiStep = it.uiStep.copy(
                                currentPassword = password,
                                hasCurrentPasswordError = false,
                                hasWrongCurrentPasswordError = false,
                            )
                        )
                    }

                    else -> it
                }
            }
        }
    }

    fun onEvent(event: RecoverKeysScreenUiEvent) {

        if (logoutUseCase.isProcessingAny() || recoverAccountUseCase.isProcessingAny()) {
            return
        }

        when (event) {
            RecoverKeysScreenUiEvent.Logout -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = MainAlertDialog.Logout) }
                }
            }

            is RecoverKeysScreenUiEvent.EditOldPassword -> onOldPasswordChange(event.password)
            is RecoverKeysScreenUiEvent.EditCurrentPassword -> onCurrentPasswordChange(event.password)
            is RecoverKeysScreenUiEvent.DialogAction -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = null) }
                }
                when (event.action.dialog) {
                    is MainAlertDialog.RecoveryResetAccount -> {
                        if (event.action.isOk) {
                            doResetAccount()
                        }
                    }

                    is MainAlertDialog.RecoveryRecoverAccount -> {
                        if (event.action.isOk) {
                            doRecoverAccount()
                        }
                    }

                    is MainAlertDialog.Logout -> {
                        if (event.action.isOk) {
                            logout()
                        }
                    }
                }
            }

            RecoverKeysScreenUiEvent.OnConfirmAction -> {
                if (_uiState.value.data?.isInProgress != false) return
                when (_uiState.value.data?.uiStep) {
                    RecoverKeysUiStep.FetchKeysFailed -> doFetchUserKeys()

                    RecoverKeysUiStep.InvalidKeysResetGuestAccount -> {
                        updateSuccessState { it.copy(alertDialog = MainAlertDialog.RecoveryResetAccount) }
                    }

                    RecoverKeysUiStep.MissingKeysResetOnlyGuest -> {
                        updateSuccessState { it.copy(alertDialog = MainAlertDialog.RecoveryResetAccount) }
                    }

                    is RecoverKeysUiStep.RecoverClassicAccountFinalStep -> {
                        if (validateCurrentPassword() && validateOldPassword()) {
                            updateSuccessState { it.copy(alertDialog = MainAlertDialog.RecoveryRecoverAccount) }
                        }
                    }

                    is RecoverKeysUiStep.RecoverGuestAccountFinalStep -> {
                        updateSuccessState { it.copy(alertDialog = MainAlertDialog.RecoveryRecoverAccount) }
                    }

                    is RecoverKeysUiStep.ResetClassicAccount -> {
                        if (validateCurrentPassword()) {
                            updateSuccessState { it.copy(alertDialog = MainAlertDialog.RecoveryResetAccount) }
                        }
                    }

                    is RecoverKeysUiStep.TryRecoverClassicAccount -> doCheckForKeyState()
                    RecoverKeysUiStep.TryRecoverGuestAccount -> doCheckForKeyState()

                    null -> {}
                }
            }

            RecoverKeysScreenUiEvent.OnSkipAction -> {
                if (_uiState.value.data?.isInProgress != false) return
                _uiState.update {
                    it.mapSuccess { state ->
                        when (state.uiStep) {
                            is RecoverKeysUiStep.ResetClassicAccount -> {
                                // without remote keys there is nothing to recover
                                if (!state.uiStep.canRecoverOldKeys) return@mapSuccess state

                                state.copy(
                                    uiStep = RecoverKeysUiStep.TryRecoverClassicAccount(
                                        oldPassword = "",
                                        hasOldPasswordError = false,
                                        hasWrongOldPasswordError = false
                                    )
                                )
                            }

                            is RecoverKeysUiStep.TryRecoverClassicAccount -> {
                                state.copy(
                                    uiStep = RecoverKeysUiStep.ResetClassicAccount(
                                        currentPassword = "",
                                        hasCurrentPasswordError = false,
                                        canRecoverOldKeys = true
                                    )
                                )
                            }

                            else -> state
                        }
                    }
                }
            }
        }
    }

    private fun updateSuccessState(action: (RecoverKeysScreenState) -> RecoverKeysScreenState) {
        _uiState.update { it.mapSuccess(action) }
    }
}