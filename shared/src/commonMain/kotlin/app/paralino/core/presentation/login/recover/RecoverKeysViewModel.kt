package app.paralino.core.presentation.login.recover

import app.paralino.core.MR
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.AppAccountStateMonitor
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.domain.user.RecoverAccountUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.dialogs.MainAlertDialog
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
import kotlinx.coroutines.launch

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
        coroutineScope.launch(Dispatchers.IO) {
            val userKeys = try {
                recoverAccountUseCase.getUserKeysFromRemote()
            } catch (e: Exception) {
                showSnackbar(e.errorString())
                emptyList()
            }

            keys = userKeys

            _uiState.update { uiState ->
                Payload.Success(
                    RecoverKeysScreenState(
                        uiStep = when {
                            userKeys.isEmpty() -> {
                                if (appPrefs.hasGuestAccount()) {
                                    RecoverKeysUiStep.Reset.ResetGuest
                                } else {
                                    RecoverKeysUiStep.Reset.ResetClassicAccount()
                                }
                            }

                            else -> {
                                if (appPrefs.hasGuestAccount()) {
                                    RecoverKeysUiStep.Recover.RecoverGuest(
                                        recoverState = Resource.NotInitialized
                                    )
                                } else {
                                    RecoverKeysUiStep.Recover.RecoverClassicAccount(
                                        recoverState = Resource.NotInitialized
                                    )
                                }
                            }
                        }
                    )
                )
            }
        }
    }

    private fun recoverAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return

        val recoverFlow = when (validState.uiStep) {
            is RecoverKeysUiStep.Recover.RecoverClassicAccount -> {
                if (validateCurrentPassword() && validateOldPassword() && validState.uiStep.recoverState.data != null) {
                    recoverAccountUseCase.recoverUserAccount(
                        oldPassword = validState.uiStep.oldPassword,
                        currentPassword = validState.uiStep.currentPassword,
                        key = validState.uiStep.recoverState.data!!
                    )
                } else {
                    null
                }
            }

            is RecoverKeysUiStep.Recover.RecoverGuest -> {
                if (validState.uiStep.recoverState.data != null) {
                    recoverAccountUseCase.recoverGuestAccount(
                        key = validState.uiStep.recoverState.data!!
                    )
                } else {
                    null
                }
            }

            is RecoverKeysUiStep.Reset.ResetClassicAccount -> null
            RecoverKeysUiStep.Reset.ResetGuest -> null
        }

        if (recoverFlow != null) {
            recoverFlow
                .mapToPayload { it }
                .onEach { result ->

                    if (result is Payload.Error) {
                        showSnackbar(result.exception.errorString())
                    }

                    _uiState.update {
                        it.mapSuccess {
                            it.copy(checkLoading = result is Payload.Loading)
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

    private fun resetAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return

        val resetFlow = when (validState.uiStep) {
            is RecoverKeysUiStep.Recover.RecoverClassicAccount -> {
                if (validState.uiStep.recoverState.data == null && validateCurrentPassword()) {
                    recoverAccountUseCase.resetUserAccount(validState.uiStep.currentPassword)
                } else {
                    null
                }
            }

            is RecoverKeysUiStep.Recover.RecoverGuest -> {
                if (validState.uiStep.recoverState.data == null) {
                    recoverAccountUseCase.resetGuestAccount()
                } else {
                    null
                }
            }

            is RecoverKeysUiStep.Reset.ResetClassicAccount -> {
                if (validateCurrentPassword()) {
                    recoverAccountUseCase.resetUserAccount(validState.uiStep.currentPassword)
                } else {
                    null
                }
            }

            RecoverKeysUiStep.Reset.ResetGuest -> {
                recoverAccountUseCase.resetGuestAccount()
            }
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
                            it.copy(resetLoading = result is Payload.Loading)
                        }
                    }
                }
                .flowOn(Dispatchers.IO)
                .launchIn(coroutineScope)
        } else {
            showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
        }
    }

    private fun checkForKey() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return

        val checkFlow: Flow<GeneratedUserKeys?>? = when (validState.uiStep) {
            is RecoverKeysUiStep.Recover.RecoverClassicAccount -> {
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

            is RecoverKeysUiStep.Recover.RecoverGuest -> {
                flow {
                    val key = recoverAccountUseCase.getCorrectGuestUserKey(keys = keys)
                    emit(key)
                }
            }

            is RecoverKeysUiStep.Reset.ResetClassicAccount -> null
            RecoverKeysUiStep.Reset.ResetGuest -> null
        }



        if (checkFlow != null) {
            checkFlow
                .mapToPayload { it }
                .onEach { result ->
                    val state = when (result) {
                        Payload.Loading -> Resource.NotInitialized
                        is Payload.Success -> {
                            Resource.Ready(result.data)
                        }

                        is Payload.Error -> Resource.Ready(null)
                    }

                    _uiState.update {
                        it.mapSuccess {
                            when (it.uiStep) {
                                is RecoverKeysUiStep.Recover.RecoverClassicAccount -> {
                                    it.copy(
                                        uiStep = it.uiStep.copy(recoverState = state),
                                        checkLoading = result is Payload.Loading
                                    )
                                }

                                is RecoverKeysUiStep.Recover.RecoverGuest -> {
                                    it.copy(
                                        uiStep = it.uiStep.copy(recoverState = state),
                                        checkLoading = result is Payload.Loading
                                    )
                                }

                                is RecoverKeysUiStep.Reset.ResetClassicAccount -> {
                                    it.copy(checkLoading = result is Payload.Loading)
                                }

                                RecoverKeysUiStep.Reset.ResetGuest -> {
                                    it.copy(checkLoading = result is Payload.Loading)
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
        val validState = (_uiState.value as? Payload.Success)?.data?.uiStep ?: return false
        if (validState !is RecoverKeysUiStep.Recover.RecoverClassicAccount) return false

        val password = validState.oldPassword
        val passwordValidation = FormValidation.TextNotEmpty.validate(password)

        _uiState.update {
            it.mapSuccess {
                if (it.uiStep is RecoverKeysUiStep.Recover.RecoverClassicAccount) {
                    it.copy(
                        uiStep = it.uiStep.copy(hasOldPasswordError = !passwordValidation.result)
                    )
                } else {
                    it
                }
            }
        }

        if (!passwordValidation.result) {
            showSnackbar(getFormValidationErrorText(passwordValidation.type))
        }

        return passwordValidation.result
    }

    private fun validateCurrentPassword(): Boolean {
        val validState = (_uiState.value as? Payload.Success)?.data?.uiStep ?: return false

        val password =
            (validState as? RecoverKeysUiStep.Recover.RecoverClassicAccount)?.currentPassword
                ?: (validState as? RecoverKeysUiStep.Reset.ResetClassicAccount)?.currentPassword
                ?: return false
        val passwordValidation = FormValidation.TextNotEmpty.validate(password)

        _uiState.update {
            it.mapSuccess {
                when (it.uiStep) {
                    is RecoverKeysUiStep.Recover.RecoverClassicAccount -> {
                        it.copy(uiStep = it.uiStep.copy(hasCurrentPasswordError = !passwordValidation.result))
                    }

                    is RecoverKeysUiStep.Reset.ResetClassicAccount -> {
                        it.copy(uiStep = it.uiStep.copy(hasCurrentPasswordError = !passwordValidation.result))
                    }

                    else -> it
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
        if (validState.actionLoading) return
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
        if (validState.actionLoading) return
        _uiState.update {
            it.mapSuccess {
                if (it.uiStep is RecoverKeysUiStep.Recover.RecoverClassicAccount) {
                    it.copy(
                        uiStep = it.uiStep.copy(
                            oldPassword = password,
                            hasOldPasswordError = false,
                            recoverState = Resource.NotInitialized
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
        if (validState.actionLoading) return
        _uiState.update {
            it.mapSuccess {
                when (it.uiStep) {
                    is RecoverKeysUiStep.Recover.RecoverClassicAccount -> {
                        it.copy(
                            uiStep = it.uiStep.copy(
                                currentPassword = password,
                                hasCurrentPasswordError = false
                            )
                        )
                    }

                    is RecoverKeysUiStep.Reset.ResetClassicAccount -> {
                        it.copy(
                            uiStep = it.uiStep.copy(
                                currentPassword = password,
                                hasCurrentPasswordError = false
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
            RecoverKeysScreenUiEvent.HideDialog -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = null) }
                }
            }

            RecoverKeysScreenUiEvent.Logout -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = MainAlertDialog.Logout) }
                }
            }

            RecoverKeysScreenUiEvent.RecoverAccount -> {
                val shouldCheckForKey = when (val step = _uiState.value.data?.uiStep) {
                    is RecoverKeysUiStep.Recover.RecoverClassicAccount -> step.recoverState.data == null
                    is RecoverKeysUiStep.Recover.RecoverGuest -> step.recoverState.data == null
                    is RecoverKeysUiStep.Reset.ResetClassicAccount,
                    RecoverKeysUiStep.Reset.ResetGuest -> return

                    null -> return
                }

                if (shouldCheckForKey) {
                    checkForKey()
                } else {
                    _uiState.update {
                        it.mapSuccess { it.copy(alertDialog = MainAlertDialog.RecoveryRecoverAccount) }
                    }
                }
            }

            RecoverKeysScreenUiEvent.ResetAccount -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = MainAlertDialog.RecoveryResetAccount) }
                }
            }

            is RecoverKeysScreenUiEvent.EditOldPassword -> onOldPasswordChange(event.password)
            is RecoverKeysScreenUiEvent.EditCurrentPassword -> onCurrentPasswordChange(event.password)
            RecoverKeysScreenUiEvent.ConfirmLogout -> logout()
            RecoverKeysScreenUiEvent.ConfirmResetAccount -> resetAccount()
            RecoverKeysScreenUiEvent.ConfirmRecoverAccount -> recoverAccount()
        }
    }
}