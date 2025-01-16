package app.paralino.core.presentation.settings.billing

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.MR
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.user.UserPlans
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.poll
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PaywallViewModel(
    val sourceType: BillingScreenSourceType,
    private val userRepo: UserRepository,
    private val refreshUseCase: RefreshEverythingUseCase,
    private val remoteConfigRepo: RemoteConfigRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    val billingProvider: IBillingProvider
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        mapUserPlans(
            plans = userRepo.userPlans.value,
            products = null,
            currentState = null,
            config = remoteConfigRepo.getCachedConfigOnce()
        )
    )
    val uiState: StateFlow<PaywallScreenState> = _uiState

    private val _eventChannel = Channel<PaywallScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        combine(
            userRepo.userPlans,
            flow {
                remoteConfigRepo.refreshConfig(cacheTimeMillis = 0)
                val products = billingProvider.configure()
                emit(products)
                _uiState.update { it.copy(isLoading = false) }
            }
        ) { plans, products ->
            _uiState.update {
                mapUserPlans(
                    plans = plans,
                    products = products,
                    currentState = it,
                    config = remoteConfigRepo.getConfigOnce()
                )
            }
        }
            .catch {
                logg.i(message = "Error getting billing ID", t = it)
                messageDispatcher.showSnackbar(text = it.errorString())
                _uiState.update { it.copy(alertDialog = MainAlertDialog.BillingLoadingError) }
            }
            .launchIn(coroutineScope)

        coroutineScope.launch {
            try {
                userRepo.refreshUserMaybe()
            } catch (e: Exception) {
                logg.d(message = "Error refreshing user and plans", t = e)
            }
        }
    }

    fun onEvent(event: PaywallScreenUiEvent) {
        when (event) {
            is PaywallScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.BillingLoadingError, is MainAlertDialog.BillingError -> {
                        _uiState.update { it.copy(alertDialog = null) }
                        coroutineScope.launch {
                            _eventChannel.send(PaywallScreenEvent.OnNavigateBack(isSuccess = false))
                        }
                    }

                    else -> {}
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
                        refreshUseCase.scheduleRefreshAllGroupsWithPlaces(cacheTimeMillis = 0)
                        _eventChannel.send(PaywallScreenEvent.OnNavigateBack(isSuccess = true))
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.BillingError(
                                    code = 0,
                                    message = MR.strings.paywall_purchase_error_alert_message.localizedString()
                                )
                            )
                        }
                    }
                }
            }

            is PaywallScreenUiEvent.SelectBillingPeriod -> {
                _uiState.update { state ->
                    state.copy(
                        plans = state.plans.map {
                            if (it.type == event.planType) {
                                it.copy(
                                    billingPeriods = it.billingPeriods.map {
                                        it.copy(isSelected = it.billingPeriod == event.period)
                                    }
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
            }

            is PaywallScreenUiEvent.SelectPlanType -> {
                _uiState.update {
                    it.copy(
                        plans = it.plans.map {
                            it.copy(
                                isSelected = it.type == event.planType,
                                isExpanded = false
                            )
                        }
                    )
                }
            }

            PaywallScreenUiEvent.StartPurchase -> {
                val selectedPlan = uiState.value.selectedPlan
                val selectedBillingPeriod = selectedPlan?.selectedBillingPeriod
                if (selectedPlan == null) {
                    messageDispatcher.showSnackbar(text = MR.strings.paywall_select_plan_label.localizedString())
                } else if (selectedPlan.type.isPremium) {
                    if (selectedBillingPeriod == null) {
                        messageDispatcher.showSnackbar(text = MR.strings.paywall_select_plan_label.localizedString())
                    } else {
                        _uiState.update { it.copy(isPurchaseButtonLoading = true) }
                        coroutineScope.launch {
                            _eventChannel.send(
                                PaywallScreenEvent.OnStartPurchase(
                                    planType = selectedPlan.type,
                                    newPlanProductId = selectedBillingPeriod.productPlanId
                                )
                            )
                        }
                    }
                } else {
                    coroutineScope.launch {
                        _eventChannel.send(PaywallScreenEvent.OnNavigateBack(isSuccess = false))
                    }
                }
            }

            PaywallScreenUiEvent.RestorePurchase -> {
                _uiState.update { it.copy(isPurchaseButtonLoading = true, isLoading = true) }
                coroutineScope.launch {
                    _eventChannel.send(PaywallScreenEvent.OnRestorePurchase)
                }
            }

            PaywallScreenUiEvent.RequestPromoCode -> {
                _uiState.update { it.copy(isPromoCodeSectionVisible = true) }
            }

            is PaywallScreenUiEvent.OnChangePromoCode -> {
                _uiState.update { it.copy(promoCode = event.code) }
            }

            PaywallScreenUiEvent.ApplyPromoCode -> {
                _uiState.update { it.copy(isPurchaseButtonLoading = true, isLoading = true) }
                coroutineScope.launch {
                    try {
                        userRepo.redeemPromoCode(code = uiState.value.promoCode)
                        onEvent(PaywallScreenUiEvent.RefreshPlans)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                alertDialog = MainAlertDialog.BillingError(
                                    code = 0,
                                    message = MR.strings.paywall_purchase_error_alert_message.localizedString()
                                )
                            )
                        }
                    }
                }
            }

            is PaywallScreenUiEvent.OnPurchaseError -> {
                if (event.error.userCancelled) {
                    _uiState.update { it.copy(isPurchaseButtonLoading = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            alertDialog = MainAlertDialog.BillingError(
                                code = event.error.errorCode,
                                message = event.error.errorMessage
                            )
                        )
                    }
                }
            }
        }
    }

    private fun mapUserPlans(
        plans: UserPlans,
        products: List<IBillingProvider.SubscriptionPlanData>?,
        currentState: PaywallScreenState?,
        config: ConfigData
    ): PaywallScreenState {

        val freePlanConfig = plans.nonExpiredPlans.firstOrNull { !it.config.planTypeEnum.isPremium }
            ?.config
            ?: config.defaultFreePlanConfig()

        val availableProducts = products
            ?.sortedBy { it.periods.minBy { it.price }.price }
            ?: emptyList()

        val isFreePlanSelected = isPlanSelected(
            plans = plans,
            planType = freePlanConfig.planTypeEnum,
            currentState = currentState,
            config = config
        )
        val freeUiPlan = PlanInfoState(
            productId = "free",
            name = if (freePlanConfig.planTypeEnum == PlanType.SILVER) {
                MR.strings.paywall_silver_plan_name.localizedString()
            } else {
                MR.strings.paywall_free_plan_name.localizedString()
            },
            type = freePlanConfig.planTypeEnum,
            isCurrentPlan = false,
            hasSpecialOffer = false,
            isSelected = isFreePlanSelected,
            isExpanded = false,
            billingPeriods = emptyList(),
            includedMembersPerGroup = freePlanConfig.membersLimit,
            benefits = freePlanConfig.planBenefits()
        )

        val uiPlans = availableProducts.map { product ->
            val planConfig = product.planConfig
            val isPlanSelected = isPlanSelected(
                plans = plans,
                planType = planConfig.planTypeEnum,
                currentState = currentState,
                config = config
            )
            val billingPeriods = product.periods
                .groupBy { it.billingPeriod }
                .map { periods ->
                    val period = periods.value.first()

                    val fullYearlyPrice = product.periods
                        .filter { it.billingPeriod == PlanBillingPeriod.MONTHLY }
                        .maxBy { it.referencePerYearPrice ?: it.price }
                        .takeIf { period.billingPeriod == PlanBillingPeriod.ANNUALLY }

                    val highestPrice = periods.value.maxBy { it.price }
                        .takeIf { periods.value.size > 1 }
                    val lowestPrice = periods.value.minBy { it.price }

                    PlanBillingPeriodState(
                        productPlanId = lowestPrice.productPlanId,
                        name = when (period.billingPeriod) {
                            PlanBillingPeriod.WEEKLY -> MR.strings.plan_period_weekly_name.localizedString()
                            PlanBillingPeriod.MONTHLY -> MR.strings.plan_period_monthly_name.localizedString()
                            PlanBillingPeriod.ANNUALLY -> MR.strings.plan_period_annually_name.localizedString()
                            PlanBillingPeriod.ONETIME -> MR.strings.plan_period_onetime_name.localizedString()
                        },
                        billingPeriod = period.billingPeriod,
                        isSelected = isBillingPeriodSelected(
                            period = period.billingPeriod,
                            planType = planConfig.planTypeEnum,
                            plans = plans,
                            currentState = currentState
                        ),
                        price = lowestPrice.formattedPrice,
                        discountedPrice = listOfNotNull(
                            fullYearlyPrice?.referencePerYearFormattedPrice,
                            highestPrice?.formattedPrice
                        )
                            .distinct()
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("\n"), // { it.filterNot { it == ' ' } },
                        discountPercent = listOfNotNull(
                            fullYearlyPrice?.referencePerYearPrice,
                            highestPrice?.price
                        )
                            .maxOrNull()
                            ?.takeIf { period.billingPeriod == PlanBillingPeriod.ANNUALLY }
                            ?.let { (1f - (lowestPrice.price.toFloat() / it)) * 100 }
                            ?.toInt()
                            // using even numbers to display discount
                            // will even out discounts across plans and still be capped below actual discount
                            ?.let { it - (it % 2) }
                            ?.let { "$it%" },
                        isPromoDiscount = highestPrice != null,
                        pricePerMonth = null,
                        trialDurationDays = lowestPrice.trialDurationDays,
                    )
                }
            PlanInfoState(
                productId = product.productId,
                name = planConfig.planName,
                type = planConfig.planTypeEnum,
                isCurrentPlan = plans.highestValidPlanConfig.planTypeEnum == planConfig.planTypeEnum,
                hasSpecialOffer = billingPeriods.any { it.isPromoDiscount },
                isSelected = isPlanSelected,
                isExpanded = sourceType != BillingScreenSourceType.ONBOARDING,
                billingPeriods = billingPeriods,
                includedMembersPerGroup = planConfig.membersLimit,
                benefits = planConfig.planBenefits()
            )
        }

        return PaywallScreenState(
            canShowMoreInfo = AppBuildConfig.instance.isDevMode,
            plans = buildList {
//                if (sourceType == BillingScreenSourceType.ONBOARDING && !plans.highestValidPlanConfig.planTypeEnum.isPremium) {
//                    add(freeUiPlan)
//                }
                addAll(uiPlans)
            },
            isLoading = currentState?.isLoading ?: true,
            alertDialog = currentState?.alertDialog,
            isPurchaseButtonLoading = currentState?.isPurchaseButtonLoading ?: false,
            faq = currentState?.faq ?: getFaqSection(config),
            isPromoCodeSectionVisible = currentState?.isPromoCodeSectionVisible ?: false,
            promoCode = currentState?.promoCode ?: ""
        )
    }

    private fun getFaqSection(config: ConfigData): FaqSectionState {
        return FaqSectionState(
            faqs = buildList {
                add(
                    FaqSectionItem(
                        title = MR.strings.paywall_faq_question_premium_group_features.string(),
                        content = MR.strings.paywall_faq_answer_premium_group_features.string(),
                        expanded = true
                    )
                )

                config.listedPlanConfigs()
                    .filter { it.isActive }
                    .forEach {
                        val planName = if (it.planTypeEnum.isPremium) {
                            it.planName
                        } else if (it.planTypeEnum == PlanType.SILVER) {
                            MR.strings.paywall_silver_plan_name.localizedString()
                        } else {
                            MR.strings.paywall_free_plan_name.localizedString()
                        }
                        add(
                            FaqSectionItem(
                                title = MR.strings.paywall_faq_question_plan_features.localizedString(
                                    planName
                                ).annotatedBold(),
                                content = MR.strings.paywall_faq_answer_plan_features.localizedString(
                                    planName,
                                    it.membersLimit,
                                    it.placesLimit
                                ).annotatedBold()
                            )
                        )
                    }

                add(
                    FaqSectionItem(
                        title = MR.strings.paywall_faq_question_free_trial.string(),
                        content = MR.strings.paywall_faq_answer_free_trial.string()
                    )
                )
                add(
                    FaqSectionItem(
                        title = MR.strings.paywall_faq_question_upgrade_downgrade.string(),
                        content = MR.strings.paywall_faq_answer_upgrade_downgrade.string()
                    )
                )
                add(
                    FaqSectionItem(
                        title = MR.strings.paywall_faq_question_how_to_cancel.string(),
                        content = MR.strings.paywall_faq_answer_how_to_cancel.string()
                    )
                )

                when (deviceApiProvider.deviceType) {
                    DeviceType.Android -> {
                        add(
                            FaqSectionItem(
                                title = MR.strings.paywall_faq_question_refund.string(),
                                content = MR.strings.paywall_faq_answer_refund_google.string()
                            )
                        )
                        add(
                            FaqSectionItem(
                                title = MR.strings.paywall_faq_question_payment_security.string(),
                                content = MR.strings.paywall_faq_answer_payment_security_google.string()
                            )
                        )
                    }

                    DeviceType.iOS -> {
                        add(
                            FaqSectionItem(
                                title = MR.strings.paywall_faq_question_refund.string(),
                                content = MR.strings.paywall_faq_answer_refund_apple.string()
                            )
                        )
                        add(
                            FaqSectionItem(
                                title = MR.strings.paywall_faq_question_payment_security.string(),
                                content = MR.strings.paywall_faq_answer_payment_security_apple.string()
                            )
                        )
                    }
                }

                add(
                    FaqSectionItem(
                        title = MR.strings.paywall_faq_question_contact_support.string(),
                        content = MR.strings.paywall_faq_answer_contact_support.string()
                    )
                )
            }
        )
    }

    private fun StringResource.string(): AnnotatedString {
        return this.localizedString().annotatedBold()
    }

    private fun isBillingPeriodSelected(
        period: PlanBillingPeriod,
        planType: PlanType,
        plans: UserPlans,
        currentState: PaywallScreenState?
    ): Boolean {
        // first keep previous ui state
        return currentState?.plans?.firstOrNull { it.type == planType }?.billingPeriods
            ?.firstOrNull { it.billingPeriod == period }
            ?.isSelected
        // after that check what user currently has as billing period
            ?: plans.highestValidPlan
                ?.takeIf { it.plan.planTypeEnum.isPremium }
                ?.takeIf { it.config.planTypeEnum == planType }
                ?.plan?.billingPeriod
                ?.let { it == period }
            // then take default
            ?: (period == PlanBillingPeriod.MONTHLY)
    }

    private fun isPlanSelected(
        plans: UserPlans,
        planType: PlanType,
        currentState: PaywallScreenState?,
        config: ConfigData
    ): Boolean {
        val currentlySelected = currentState?.plans?.firstOrNull { it.type == planType }
        if (currentlySelected != null) {
            return currentlySelected.isSelected
        } else if (sourceType == BillingScreenSourceType.ONBOARDING) {
            if (plans.highestValidPlanConfig.planTypeEnum.isPremium) {
                return false
            } else {
                // if user does not have a premium plan, pre-select lowest premium tier
                return config.listedPlanConfigs()
                    .filter { it.planTypeEnum.isPremium }
                    .minByOrNull { it.planTypeEnum.order }?.planTypeEnum == planType
            }
        } else {
            return false
        }
    }
}