package app.paralino.core.presentation.settings.subscriptions

import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.extensions.errorString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val userRepo: UserRepository,
    private val billingProvider: IBillingProvider
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        PaywallScreenState(
            isLoading = true,
            canShowMoreInfo = AppBuildConfig.instance.isDevMode
        )
    )
    val uiState: StateFlow<PaywallScreenState> = _uiState

    private val _eventChannel = Channel<PaywallScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        coroutineScope.launch {
            try {
                billingProvider.configure(waitIfNeeded = true)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                logg.i(message = "Error getting billing ID", t = e)
                messageDispatcher.showSnackbar(text = e.errorString())
                _uiState.update { it.copy(alertDialog = MainAlertDialog.BillingLoadingError) }
            }
        }
    }

    fun onEvent(event: PaywallScreenUiEvent) {
        when (event) {
            is PaywallScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.BillingLoadingError -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        _eventChannel.trySend(PaywallScreenEvent.OnNavigateBack)
                    }

                    else -> {

                    }
                }
            }
        }
    }
}