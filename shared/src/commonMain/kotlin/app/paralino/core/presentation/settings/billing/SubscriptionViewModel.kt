package app.paralino.core.presentation.settings.billing

import app.paralino.core.MR
import app.paralino.core.domain.user.GetDirectCheckoutWebPortalUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.Payload
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.user.UserPlan
import app.paralino.core.models.user.UserPlans
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SubscriptionViewModel(
    private val appConfig: AppConfigRepository,
    private val userRepo: UserRepository,
    private val userConfigRepo: UserConfigRepository,
    private val deviceApi: IDeviceApiProvider,
    private val getWebPortal: GetDirectCheckoutWebPortalUseCase,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionScreenState(
            allActivePlans = getAllActivePlansState(
                plans = userRepo.userPlans.value,
                previousState = null
            ),
            isLoading = true,
            currentPlanName = userRepo.userPlans.value.highestValidPlanConfig.adjustedPlanName,
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
                        allActivePlans = getAllActivePlansState(
                            plans = plans,
                            previousState = state
                        ),
                        currentPlanName = plans.highestValidPlanConfig.adjustedPlanName,
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

    private fun getAllActivePlansState(
        plans: UserPlans,
        previousState: SubscriptionScreenState?
    ): List<SubscriptionDetailsState> {
        return plans.nonExpiredPlans
            .filter { it.config.planTypeEnum.isPremium }
            .sortedByDescending { it.config.planTypeEnum.order }
            .map { plan ->
                getPlanState(
                    plan = plan,
                    isCurrentPlan = plan.plan.id == plans.highestValidPlan?.plan?.id,
                    isManageLoading = previousState?.allActivePlans?.firstOrNull {
                        it.planId == plan.plan.id
                    }?.isManageLoading ?: false,
                    config = plan.config
                )
            }
    }

    private fun getPlanState(
        plan: UserPlan?,
        isCurrentPlan: Boolean,
        isManageLoading: Boolean,
        config: PlanConfig
    ): SubscriptionDetailsState {
        return SubscriptionDetailsState(
            planId = plan?.plan?.id ?: "free",
            isCurrentPlan = isCurrentPlan,
            isPremiumPlan = config.planTypeEnum.isPremium,
            isManageLoading = isManageLoading,
            canManagePlanOnThisDevice = when (plan?.plan?.provider) {
                PlanProvider.APPLE_APP_STORE -> deviceApi.deviceType == DeviceType.iOS
                PlanProvider.GOOGLE_PLAY_STORE -> appConfig.appFlavor == AppFlavor.ANDROID_GOOGLE
                PlanProvider.WEB_POLAR -> appConfig.appFlavor == AppFlavor.ANDROID_DIRECT
                PlanProvider.PARALINO -> null
                null -> null
            },
            canChangePlanOnThisDevice = when (plan?.plan?.provider) {
                PlanProvider.APPLE_APP_STORE -> deviceApi.deviceType == DeviceType.iOS
                PlanProvider.GOOGLE_PLAY_STORE -> appConfig.appFlavor == AppFlavor.ANDROID_GOOGLE
                PlanProvider.WEB_POLAR -> false
                PlanProvider.PARALINO -> false
                null -> false
            },
            planName = config.adjustedPlanName,
            billingPeriod = when (plan?.plan?.billingPeriod) {
                PlanBillingPeriod.WEEKLY -> MR.strings.plan_period_weekly_name.localizedString()
                PlanBillingPeriod.MONTHLY -> MR.strings.plan_period_monthly_name.localizedString()
                PlanBillingPeriod.ANNUALLY -> MR.strings.plan_period_annually_name.localizedString()
                PlanBillingPeriod.ONETIME -> "∅"
                else -> null
            },
            nextRenewal = if (plan?.plan?.cancelledAt == null && plan?.plan?.billingPeriod?.isRenewalBased == true) {
                plan.plan.expiresAt?.let { expiresAt ->
                    valueFormatter.formatSimpleDateTime(
                        timeUTC = expiresAt.toLocalDateTime(TimeZone.UTC),
                        optionalYear = false,
                        optionalMonth = false,
                        showTodayInfo = true
                    )
                }
            } else {
                null
            },
            expiresAt = if (plan?.plan?.cancelledAt != null || plan?.plan?.billingPeriod?.isRenewalBased == false) {
                plan.plan.expiresAt?.let { expiresAt ->
                    valueFormatter.formatSimpleDateTime(
                        timeUTC = expiresAt.toLocalDateTime(TimeZone.UTC),
                        optionalYear = false,
                        optionalMonth = false,
                        showTodayInfo = true
                    )
                } ?: "∅"
            } else {
                null
            },
            planProviderText = when (plan?.plan?.provider) {
                PlanProvider.APPLE_APP_STORE -> {
                    MR.strings.plans_management_providerInfo_apple.localizedString()
                }

                PlanProvider.GOOGLE_PLAY_STORE -> {
                    MR.strings.plans_management_providerInfo_google.localizedString()
                }

                PlanProvider.WEB_POLAR -> {
                    MR.strings.plans_management_providerInfo_polar.localizedString()
                }

                PlanProvider.PARALINO -> {
                    MR.strings.plans_management_providerInfo_paralino.localizedString()
                }

                else -> null
            }
        )
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

            is SubscriptionScreenUiEvent.OnManagePlanClick -> {
                val state = uiState.value
                if (state.allActivePlans.any { it.isManageLoading }) return
                if (!state.canManagePlanOnThisDevice) return

                when (appConfig.appFlavor) {
                    AppFlavor.ANDROID_DIRECT -> {
                        requestDirectBillingUrl(planId = event.planId)
                    }

                    AppFlavor.ANDROID_GOOGLE,
                    AppFlavor.APPLE_IOS -> {
                        _eventChannel.trySend(SubscriptionScreenEvent.OpenNativeBillingStorePage)
                    }
                }
            }
        }
    }

    private fun requestDirectBillingUrl(planId: String) {
        getWebPortal.getWebPortalUrl()
            .mapToPayload { it }
            .onEach { result ->
                _uiState.update { state ->
                    state.copy(
                        allActivePlans = state.allActivePlans.map { plan ->
                            if (plan.planId == planId && plan.canManagePlanOnThisDevice == true) {
                                plan.copy(isManageLoading = result is Payload.Loading)
                            } else {
                                plan
                            }
                        }
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