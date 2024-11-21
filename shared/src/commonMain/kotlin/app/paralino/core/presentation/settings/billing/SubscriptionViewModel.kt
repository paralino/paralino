package app.paralino.core.presentation.settings.billing

import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.user.UserPlan
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val userRepo: UserRepository,
    private val userConfigRepo: UserConfigRepository,
    private val deviceApi: IDeviceApiProvider,
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionScreenState(
            subscriptionState = getPlanState(userRepo.userPlans.value.highestValidPlan),
            isLoading = true,
            canShowMoreInfo = AppBuildConfig.instance.isDevMode
        )
    )
    val uiState: StateFlow<SubscriptionScreenState> = _uiState

    private val _eventChannel = Channel<SubscriptionScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepo.userPlans
            .onEach { plans ->
                _uiState.update { it.copy(subscriptionState = getPlanState(plans.highestValidPlan)) }
            }
            .launchIn(coroutineScope)

        coroutineScope.launch {
            try {
                userRepo.refreshUserMaybe()
            } catch (e: Exception) {
                logg.d(message = "Error refreshing user and plans", t = e)
            }
        }

        coroutineScope.launch {
            try {
                userConfigRepo.ensureAndGetBillingUserId()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                logg.w(message = "Error getting billing ID", t = e)
            }
        }
    }

    private fun getPlanState(plan: UserPlan?): SubscriptionInfoState? {
        return plan?.let {
            SubscriptionInfoState(
                name = it.config.planName,
                expiresAt = it.plan.expiresAt?.toString() ?: "N/A",
                billingPeriod = it.plan.billingPeriod.name,
                hasPremiumPlan = it.plan.planTypeEnum.isPremium,
                isCurrentPlatform = when (it.plan.provider) {
                    PlanProvider.APPLE_APP_STORE -> deviceApi.deviceType == DeviceType.iOS
                    PlanProvider.GOOGLE_PLAY_STORE -> deviceApi.deviceType == DeviceType.Android
                    PlanProvider.PARALINO -> false
                },
                isFromParalino = it.plan.provider == PlanProvider.PARALINO
            )
        }
    }

    fun onEvent(event: SubscriptionScreenUiEvent) {
        when (event) {
            is SubscriptionScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.BillingLoadingError -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        _eventChannel.trySend(SubscriptionScreenEvent.OnNavigateBack)
                    }

                    else -> {

                    }
                }
            }
        }
    }
}