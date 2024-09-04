package app.paralino.core.presentation.settings.subscriptions

import app.paralino.core.data.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.user.UserPlan
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class SubscriptionViewModel(
    private val userRepo: UserRepository,
    private val billingProvider: IBillingProvider,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val appBuildConfig: AppBuildConfig,
    private val deviceApi: IDeviceApiProvider,
    private val appPrefs: AppPrefs
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionScreenState(
            subscriptionState = getPlanState(userRepo.userPlans.value.highestPlan)
        )
    )
    val uiState: Flow<SubscriptionScreenState> = _uiState

    private val _eventChannel = Channel<SubscriptionScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepo.userPlans
            .onEach { plans ->
                _uiState.update { it.copy(subscriptionState = getPlanState(plans.highestPlan)) }
            }
            .launchIn(coroutineScope)

        // TODO change to billing user id
        billingProvider.configure(billingUserId = userRepo.cachedUserId)
    }

    private fun getPlanState(plan: UserPlan?): SubscriptionInfoState? {
        return plan?.let {
            SubscriptionInfoState(
                name = it.config.planName,
                expiresAt = it.plan.expiresAt?.toString() ?: "N/A",
                billingPeriod = it.plan.billingPeriod.name,
                hasPremiumPlan = it.plan.planTypeEnum.order > PlanType.SILVER.order,
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
                    else -> {

                    }
                }
            }
        }
    }
}