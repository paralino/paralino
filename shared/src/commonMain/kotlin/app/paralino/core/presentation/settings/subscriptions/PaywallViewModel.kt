package app.paralino.core.presentation.settings.subscriptions

import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.poll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PaywallViewModel(
    private val userRepo: UserRepository,
    private val refreshUseCase: RefreshEverythingUseCase,
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

            PaywallScreenUiEvent.RefreshPlans -> {
                _uiState.update { it.copy(isLoading = true) }
                coroutineScope.launch {
                    val currentPlan = userRepo.userPlans.value.highestValidPlan
                    try {
                        poll(
                            firstDelayMillis = 3.seconds.inWholeMilliseconds,
                            delay = 2.seconds.inWholeMilliseconds
                        ) {
                            userRepo.refreshUserMaybe(timeDelayMillis = 0)
                            delay(200)
                            val newPlan = userRepo.userPlans.value.highestValidPlan
                            if (newPlan != currentPlan) {
                                newPlan
                            } else {
                                null
                            }
                        }

                        // at this point plans are refreshed and are different
                        // refresh groups as well and we can close the screen
                        refreshUseCase.scheduleRefreshAllGroupsWithPlaces(time = 0)
                        _eventChannel.send(PaywallScreenEvent.OnNavigateBack)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(alertDialog = MainAlertDialog.BillingLoadingError) }
                    }
                }
            }
        }
    }
}