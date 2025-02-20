package app.paralino.core.presentation.login.emailverify

import app.paralino.core.MR
import app.paralino.core.domain.user.LogoutUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.exception
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.main.DeeplinkType
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.getFormValidationErrorText
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class VerifyEmailViewModel(
    private val userRepo: UserRepository,
    private val logoutUseCase: LogoutUseCase,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        VerifyEmailScreenState(
            currentEmail = userRepo.cachedUser?.email ?: ""
        )
    )
    val uiState: StateFlow<VerifyEmailScreenState> = _uiState

    private val _eventChannel = Channel<VerifyEmailScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepo.localUser
            .onEach {
                it.data?.let { user ->
                    _uiState.update { it.copy(currentEmail = user.email) }
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: VerifyEmailScreenUiEvent) {
        when (event) {
            is VerifyEmailScreenUiEvent.EditEmailVerification -> onEmailVerificationChange(
                emailVerification = event.verification
            )

            VerifyEmailScreenUiEvent.Logout -> {
                _uiState.update { it.copy(alertDialog = MainAlertDialog.Logout) }
            }

            is VerifyEmailScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.Logout -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            logout()
                        }
                    }

                    is MainAlertDialog.VerifyEmail -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        if (event.action.action == MainAlertDialog.Buttons.buttonOk) {
                            sendEmailVerification()
                        }
                    }

                    else -> {}
                }
            }

            VerifyEmailScreenUiEvent.ResendVerification -> {
                _uiState.update {
                    it.copy(alertDialog = MainAlertDialog.VerifyEmail(email = it.currentEmail))
                }
            }

            VerifyEmailScreenUiEvent.CheckVerification -> checkEmailVerification()
            VerifyEmailScreenUiEvent.EditCurrentEmail -> _eventChannel.trySend(
                VerifyEmailScreenEvent.EditEmail
            )

            VerifyEmailScreenUiEvent.ToggleManualVerificationInput -> {
                if (!_uiState.value.isLoadingAny) {
                    _uiState.update { it.copy(isEmailVerificationShown = true) }
                }
            }
        }
    }

    private fun sendEmailVerification() {
        coroutineScope.launch {
            try {
                _uiState.update { it.copy(actionLoading = true) }
                userRepo.initEmailVerification()
                _uiState.update { it.copy(actionLoading = false) }
                messageDispatcher.showSnackbar(
                    text = MR.strings.info_email_check_instructions_popup.localizedString(),
                    duration = SnackbarDuration.Long
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(actionLoading = false) }
                messageDispatcher.showSnackbar(e.errorString())
            }
        }
    }

    private fun checkEmailVerification() {
        if (_uiState.value.isLoadingAny) {
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(fullLoading = true) }
                try {
                    withTimeout(10.seconds) {
                        userRepo.refreshUserIfAvailable()
                    }
                } catch (e: Exception) {
                }

                if (userRepo.cachedUser?.needsEmailVerification == true) {
                    checkManualEmailVerification()
                }

                _uiState.update { it.copy(fullLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(fullLoading = false) }
                showSnackbar(e.errorString())
            }
        }
    }

    private suspend fun checkManualEmailVerification() {
        val emailVerification = DeeplinkType.parse(deeplink = _uiState.value.emailVerification)

        _uiState.update {
            it.copy(
                hasEmailVerificationError = emailVerification !is DeeplinkType.EmailVerify
            )
        }

        if (emailVerification is DeeplinkType.EmailVerify) {
            if (emailVerification.userId.isNotBlank() && emailVerification.secret.isNotBlank()) {
                try {
                    _uiState.update { it.copy(fullLoading = true) }
                    userRepo.confirmEmail(
                        userId = emailVerification.userId,
                        secret = emailVerification.secret
                    )
                    userRepo.refreshUserIfAvailable()
                    _uiState.update { it.copy(fullLoading = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(fullLoading = false) }
                    showSnackbar(e.errorString())
                }
            } else {
                showSnackbar(getFormValidationErrorText(validation = FormValidation.TextNotEmpty))
            }
        } else {
            showSnackbar(getFormValidationErrorText(validation = FormValidation.TextNotEmpty))
        }
    }

    private fun onEmailVerificationChange(emailVerification: String) {
        if (_uiState.value.isLoadingAny) return
        _uiState.update {
            it.copy(
                emailVerification = emailVerification,
                hasEmailVerificationError = false
            )
        }
    }

    private fun logout() {
        if (_uiState.value.isLoadingAny || logoutUseCase.isProcessingAny()) {
            return
        }

        flow {
            logoutUseCase.fullLogout(logoutLocallyOnError = false)
            emit(Unit)
        }
            .mapToPayload { Unit }
            .onEach { state ->
                state.exception?.let { showSnackbar(it.errorString()) }
                _uiState.update {
                    it.copy(fullLoading = state is Payload.Loading)
                }
            }
            .launchIn(coroutineScope)
    }
}