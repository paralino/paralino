package app.paralino.core.presentation.settings.billing

import app.paralino.core.domain.user.GetDirectCheckoutWebPortalUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.Payload
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.user.UserPlan
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.AppSnackbarPayload
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val appConfig: AppConfigRepository,
    private val userRepo: UserRepository,
    private val userConfigRepo: UserConfigRepository,
    private val deviceApi: IDeviceApiProvider,
    private val getWebPortal: GetDirectCheckoutWebPortalUseCase
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionScreenState(
            subscriptionState = getPlanState(
                plan = userRepo.userPlans.value.highestValidPlan,
                previousState = null
            ),
            isLoading = true,
            canShowMoreInfo = appConfig.isInAppDevMode,
            featureTableState = constructFeatureTable()
        )
    )
    val uiState: StateFlow<SubscriptionScreenState> = _uiState

    private val _eventChannel = Channel<SubscriptionScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        userRepo.userPlans
            .onEach { plans ->
                _uiState.update { state ->
                    state.copy(
                        subscriptionState = getPlanState(
                            plan = plans.highestValidPlan,
                            previousState = state.subscriptionState
                        ),
                        featureTableState = constructFeatureTable()
                    )
                }
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

    private fun getPlanState(
        plan: UserPlan?,
        previousState: SubscriptionInfoState?
    ): SubscriptionInfoState? {
        return plan?.let {
            SubscriptionInfoState(
                name = it.config.adjustedPlanName,
                expiresAt = it.plan.expiresAt?.toString() ?: "N/A",
                billingPeriod = it.plan.billingPeriod.name,
                hasPremiumPlan = it.plan.planTypeEnum.isPremium,
                canManagePlan = when (it.plan.provider) {
                    PlanProvider.APPLE_APP_STORE -> deviceApi.deviceType == DeviceType.iOS

                    PlanProvider.GOOGLE_PLAY_STORE -> when (appConfig.appFlavor) {
                        AppFlavor.ANDROID_GOOGLE -> true
                        AppFlavor.ANDROID_DIRECT,
                        AppFlavor.APPLE_IOS -> false
                    }

                    PlanProvider.WEB_POLAR -> when (appConfig.appFlavor) {
                        AppFlavor.ANDROID_DIRECT -> true
                        AppFlavor.ANDROID_GOOGLE,
                        AppFlavor.APPLE_IOS -> false
                    }

                    PlanProvider.PARALINO -> false
                },
                canChangePlanOnDevice = when (it.plan.provider) {
                    PlanProvider.APPLE_APP_STORE -> deviceApi.deviceType == DeviceType.iOS
                    PlanProvider.GOOGLE_PLAY_STORE -> appConfig.appFlavor == AppFlavor.ANDROID_GOOGLE
                    PlanProvider.WEB_POLAR,
                    PlanProvider.PARALINO -> false
                },
                isManageLoading = previousState?.isManageLoading ?: false,
                isFromParalino = it.plan.provider == PlanProvider.PARALINO
            )
        }
    }

    private fun constructFeatureTable(): FeatureTableState {
        val currentPlan = userRepo.userPlans.value.highestValidPlanConfig
        return FeatureTableState.constructFeaturesTable(
            isGroupPlanOnly = false,
            plans = listOf(
                PlanInfoState(
                    productId = "",
                    name = currentPlan.adjustedPlanName,
                    config = currentPlan,
                    isCurrentPlan = true,
                    currentPlanProvider = userRepo.userPlans.value.highestValidPlan?.plan?.provider,
                    hasSpecialOffer = false,
                    isSelected = true,
                    isExpanded = true,
                    billingPeriods = emptyList()
                )
            )
        )
    }

    fun onEvent(event: SubscriptionScreenUiEvent) {
        when (event) {
            is SubscriptionScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.BillingLoadingError -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        _eventChannel.trySend(SubscriptionScreenEvent.OnNavigateBack)
                    }

                    else -> {}
                }
            }

            is SubscriptionScreenUiEvent.OnCellItemClick -> {
                when (event.cellItem) {
                    is FeatureTableItemValue.MoreInfo -> {
                        showSnackbar(
                            data = AppSnackbarData(
                                payload = AppSnackbarPayload(
                                    title = event.cellItem.title,
                                    text = event.cellItem.content,
                                    hasCloseButton = true
                                ),
                                duration = SnackbarDuration.Long
                            )
                        )
                    }
                    else -> {}
                }
            }

            SubscriptionScreenUiEvent.OnManagePlanClick -> {
                val state = uiState.value.subscriptionState ?: return
                if (state.isManageLoading) return
                if (!state.canManagePlan) return

                when (appConfig.appFlavor) {
                    AppFlavor.ANDROID_DIRECT -> {
                        requestDirectBillingUrl()
                    }

                    AppFlavor.ANDROID_GOOGLE,
                    AppFlavor.APPLE_IOS -> {
                        _eventChannel.trySend(SubscriptionScreenEvent.OpenNativeBillingStorePage)
                    }
                }
            }
        }
    }

    private fun requestDirectBillingUrl() {
        getWebPortal.getWebPortalUrl()
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update {
                    it.copy(
                        subscriptionState = it.subscriptionState?.copy(
                            isManageLoading = result is Payload.Loading
                        )
                    )
                }
                when (result) {
                    is Payload.Success -> {
                        _eventChannel.trySend(
                            SubscriptionScreenEvent.OpenDirectPlanManagementPage(url = result.data.url)
                        )
                    }

                    Payload.Loading -> {}

                    is Payload.Error<*> -> {
                        showSnackbar(result.exception.errorString())
                    }
                }
            }
            .launchIn(coroutineScope)
    }
}