package app.paralino.core.presentation.settings.billing.licence

import app.paralino.core.MR
import app.paralino.core.domain.user.BillingRefreshMonitor
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LicenceCodeViewModel(
    private val userRepo: UserRepository,
    private val billingRefreshMonitor: BillingRefreshMonitor
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(LicenceCodeState())
    val uiState: StateFlow<LicenceCodeState> = _uiState

    private val _eventChannel = Channel<LicenceCodeEvent>()
    val events = _eventChannel.receiveAsFlow()

    fun onEvent(event: LicenceCodeUiEvent) {
        when (event) {
            is LicenceCodeUiEvent.OnCodeChange -> {
                if (!uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            code = event.code,
                            stepText = null,
                            isErrorStep = false
                        )
                    }
                }
            }

            LicenceCodeUiEvent.OnSubmit -> {
                submitCode()
            }

            LicenceCodeUiEvent.OnBackClick -> {
                if (!uiState.value.isLoading) {
                    _eventChannel.trySend(LicenceCodeEvent.OnNavigateBack)
                }
            }
        }
    }

    private fun submitCode() {

        if (uiState.value.isLoading) {
            return
        }

        val code = uiState.value.code
        if (code.isBlank()) {
            _uiState.update {
                it.copy(isErrorStep = true)
            }
            messageDispatcher.showSnackbar(MR.strings.form_generic_validation.localizedString())
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                stepText = MR.strings.licence_loadingStep_activating.localizedString(),
                isErrorStep = false
            )
        }

        coroutineScope.launch {
            try {
                userRepo.redeemPromoCode(code)
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        stepText = MR.strings.licence_loadingStep_confirming.localizedString(),
                        isErrorStep = false
                    )
                }
                billingRefreshMonitor.triggerRefresh()
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        stepText = MR.strings.licence_loadingStep_activated.localizedString(),
                        isErrorStep = false
                    )
                }
                messageDispatcher.showSnackbar(MR.strings.licence_success_message.localizedString())
                delay(500)
                _eventChannel.send(LicenceCodeEvent.OnSuccess)
            } catch (e: Exception) {

                val errorMessage = if (e.asRemoteException()?.type == "license_error") {
                    when (e.asRemoteException()?.code) {
                        400 -> MR.strings.licence_error_invalid.localizedString()
                        402 -> MR.strings.licence_error_expired.localizedString()
                        403 -> MR.strings.licence_error_inUse.localizedString()
                        404 -> MR.strings.licence_error_notFound.localizedString()
                        409 -> MR.strings.licence_error_planActive.localizedString()
                        else -> e.errorString()
                    }
                } else {
                    e.errorString()
                }

                val stepText = if (e.asRemoteException()?.type == "license_error") {
                    when (e.asRemoteException()?.code) {
                        400 -> MR.strings.licence_loadingStep_invalid.localizedString()
                        402 -> MR.strings.licence_loadingStep_expired.localizedString()
                        403 -> MR.strings.licence_loadingStep_inUse.localizedString()
                        404 -> MR.strings.licence_loadingStep_notFound.localizedString()
                        409 -> MR.strings.licence_loadingStep_planActive.localizedString()
                        else -> null
                    }
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stepText = stepText,
                        isErrorStep = true
                    )
                }
                messageDispatcher.showSnackbar(errorMessage)
            }
        }
    }
}
