package app.paralino.core.presentation.login.recover

import app.paralino.core.MR
import app.paralino.core.config
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
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecoverKeysViewModel(
    val logoutUseCase: LogoutUseCase,
    val recoverAccountUseCase: RecoverAccountUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<RecoverKeysScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<RecoverKeysScreenState>> = _uiState

    private val _eventChannel = Channel<RecoverKeysScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private var keys = listOf<UserCryptoKeyRaw>()

    init {
        coroutineScope.launch(Dispatchers.IO) {
            val userKeys = try {
                recoverAccountUseCase.getUserKeys()
            } catch (e: Exception) {
                showSnackbar(e.errorString())
                emptyList()
            }

            _uiState.update { uiState ->
                val masterAuthKey = config.temporaryMasterAuthKey
                if (masterAuthKey.isNotBlank()) {
                    keys = userKeys
                    Payload.Success(
                        RecoverKeysScreenState(
                            canRecoverOldKey = userKeys.isNotEmpty(),
                            masterAuthKey = masterAuthKey
                        )
                    )
                } else {
                    Payload.Error(MissingCryptoKeyException("Missing backup password"))
                }
            }
        }
    }

    private fun recoverAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        val key = validState.recoverState.data
        if (validatePassword()) {
            if (validState.canRecoverOldKey && key != null) {
                flow {
                    recoverAccountUseCase.recoverUserAccount(
                        masterAuthKey = validState.masterAuthKey,
                        key = key
                    )
                    emit(Unit)
                }
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
                    }
                    .launchIn(coroutineScope)
            } else {
                showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
            }
        }
    }

    private fun resetAccount() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (!validState.canRecoverOldKey || validState.recoverState.data == null) {
            recoverAccountUseCase.resetUserAccount(
                masterAuthKey = validState.masterAuthKey
            )
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
                .launchIn(coroutineScope)
        } else {
            showSnackbar(MR.strings.form_invalid_credentials_description.localizedString())
        }
    }

    private fun checkForKey() {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return
        if (validatePassword()) {
            flow {
                val key =
                    recoverAccountUseCase.getCorrectUserKeyForPassword(keys, validState.oldPassword)
                emit(key)
            }
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
                            it.copy(recoverState = state, checkLoading = result is Payload.Loading)
                        }
                    }
                }
                .launchIn(coroutineScope)
        }
    }

    private fun validatePassword(): Boolean {
        val validState = (_uiState.value as? Payload.Success)?.data ?: return false
        val passwordOld = validState.oldPassword
        val oldPasswordValidation = FormValidation.Password.validate(passwordOld)

        _uiState.update {
            it.mapSuccess {
                it.copy(hasOldPasswordError = !oldPasswordValidation.result)
            }
        }

        if (!oldPasswordValidation.result) {
            showSnackbar(getFormValidationErrorText(oldPasswordValidation.type))
        }

        return oldPasswordValidation.result
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
                it.copy(
                    oldPassword = password,
                    hasOldPasswordError = false,
                    recoverState = Resource.NotInitialized
                )
            }
        }
    }

    fun onEvent(event: RecoverKeysScreenUiEvent) {
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
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = MainAlertDialog.RecoveryRecoverAccount) }
                }
            }

            RecoverKeysScreenUiEvent.ResetAccount -> {
                _uiState.update {
                    it.mapSuccess { it.copy(alertDialog = MainAlertDialog.RecoveryResetAccount) }
                }
            }

            is RecoverKeysScreenUiEvent.EditOldPassword -> onOldPasswordChange(event.password)
            RecoverKeysScreenUiEvent.CheckEncryptionKey -> checkForKey()
            RecoverKeysScreenUiEvent.ConfirmLogout -> logout()
            RecoverKeysScreenUiEvent.ConfirmResetAccount -> resetAccount()
            RecoverKeysScreenUiEvent.ConfirmRecoverAccount -> recoverAccount()
        }
    }
}