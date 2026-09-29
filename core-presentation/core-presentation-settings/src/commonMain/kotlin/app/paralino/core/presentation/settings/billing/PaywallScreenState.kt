package app.paralino.core.presentation.settings.billing

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import app.paralino.core.MR
import app.paralino.core.models.AppFlavor
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.base.SubscriptionInfoNotice
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.AlertDialogButtonData
import app.paralino.core.presentation.common.AlertDialogData
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.MainAlertDialog.Buttons
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.billing.FeatureTableItemHighlightMode
import app.paralino.core.presentation.billing.FeatureTableItemState
import app.paralino.core.presentation.billing.FeatureTableItemValue
import app.paralino.core.presentation.billing.FeatureTableState
import app.paralino.core.presentation.billing.PlanBillingPeriodState
import app.paralino.core.presentation.billing.PlanInfoState
import org.jetbrains.compose.resources.DrawableResource

data class PaywallScreenState(
    val freePlanInfo: PlanInfoState,
    val plans: List<PlanInfoState>,
    val canShowMoreInfo: Boolean,
    val canCloseScreen: Boolean,
    val isLoading: Boolean,
    val isPurchaseButtonLoading: Boolean,
    val alertDialog: MainAlertDialog?,
    val faq: FaqSectionState?,
    val appFlavor: AppFlavor,
    val isPlatformBillingSupported: Boolean,
    val canRestorePurchases: Boolean,
    val screenStep: PaywallScreenStep,
    val screenType: BillingScreenSourceType,
    val memberImageModel: ImageModel,
    val memberName: String,
    val memberDeviceName: String,
    val locationHistoryStepMessage: String,
    val subscriptionNotice: List<SubscriptionInfoNotice>,
    val subscriptionOfferDialog: MainAlertDialog.SubscriptionOfferDialog?,
    val currentPlanProvider: PlanProvider?,
) {

    val currentPlan: PlanInfoState? = plans.firstOrNull { it.isCurrentPlan }
    val currentPlanName: String = currentPlan?.let { "**${it.name}**" } ?: ""
    val hasActivePlanOnAnotherPlatform: Boolean = if (currentPlan != null && currentPlanProvider != null) {
        when (appFlavor) {
            AppFlavor.ANDROID_DIRECT -> currentPlanProvider != PlanProvider.WEB_POLAR
            AppFlavor.ANDROID_GOOGLE -> currentPlanProvider != PlanProvider.GOOGLE_PLAY_STORE
            AppFlavor.APPLE_IOS -> currentPlanProvider != PlanProvider.APPLE_APP_STORE
        }
    } else {
        false
    }

    val selectedPlan: PlanInfoState? = plans.firstOrNull { it.isSelected }
    val selectedBillingPeriod: PlanBillingPeriodState? = selectedPlan?.selectedBillingPeriod?.takeIf {
        isPlatformBillingSupported
    }

    val canShowAdditionalPlanSelector: Boolean = !screenType.isOnboarding && isPlatformBillingSupported
    val canShowPlanPeriodSelector: Boolean = isPlatformBillingSupported && selectedPlan != null
    val showExternalPricingInfo: Boolean = !isPlatformBillingSupported

    val continueButtonType: PaywallContinueButtonType = when (screenStep) {
        PaywallScreenStep.PAYWALL -> {
            val trialDurationDays = selectedBillingPeriod?.trialDurationDays
            if (!hasActivePlanOnAnotherPlatform && trialDurationDays != null) {
                PaywallContinueButtonType.FreeTrial(trialDays = trialDurationDays)
            } else {
                when (appFlavor) {
                    AppFlavor.ANDROID_GOOGLE -> {
                        // continue regardless of current plan
                        PaywallContinueButtonType.Continue
                    }

                    AppFlavor.ANDROID_DIRECT -> {
                        when (currentPlanProvider) {
                            PlanProvider.WEB_POLAR -> {
                                PaywallContinueButtonType.ManageExternal
                            }

                            else -> {
                                PaywallContinueButtonType.ContinueExternal
                            }
                        }
                    }

                    AppFlavor.APPLE_IOS -> {
                        // continue regardless of current plan
                        PaywallContinueButtonType.Continue
                    }
                }
            }
        }

        else -> {
            PaywallContinueButtonType.Continue
        }
    }

    val fullFeatureTable: FeatureTableState = FeatureTableState.constructFeaturesTable(
        freeUiPlan = freePlanInfo,
        uiPlans = plans
    )

    val featureTableForSelectedPlan: FeatureTableState? = selectedPlan?.let { plan ->
        FeatureTableState(
            canExpand = true,
            isGroupPlanOnly = false,
            features = fullFeatureTable.features.map {
                val newValues = it.values.mapNotNull {
                    if (it.planType == plan.type) {
                        when (it) {
                            is FeatureTableItemValue.Count -> it.copy(highlightMode = FeatureTableItemHighlightMode.NONE)
                            is FeatureTableItemValue.Emoji -> it.copy(highlightMode = FeatureTableItemHighlightMode.NONE)
                            is FeatureTableItemValue.Summary -> it.copy(highlightMode = FeatureTableItemHighlightMode.NONE)
                            is FeatureTableItemValue.Text -> it.copy(highlightMode = FeatureTableItemHighlightMode.NONE)
                            is FeatureTableItemValue.MoreInfo -> it.copy(highlightMode = FeatureTableItemHighlightMode.NONE)
                            is FeatureTableItemValue.Title -> null
                        }
                    } else {
                        null
                    }
                }
                when (it) {
                    is FeatureTableItemState.Section -> it
                    is FeatureTableItemState.Detail -> it.copy(values = newValues)
                    is FeatureTableItemState.Title -> it.copy(values = newValues)
                    is FeatureTableItemState.Summary -> {
                        it.copy(
                            values = newValues.filterIsInstance<FeatureTableItemValue.Summary>()
                        )
                    }
                }
            }
        )
    }

    val subscriptionNoticeForCurrentPlan: String? = selectedPlan?.let { plan ->
        plan.selectedBillingPeriod?.let {
            noticeForPlan(planType = plan.config.planTypeEnum, period = it.billingPeriod)
        }
    }

    private fun noticeForPlan(
        planType: PlanType,
        period: PlanBillingPeriod
    ): String? {
        val language = try {
            Locale.current.language
        } catch (e: Exception) {
            "en"
        }
        return subscriptionNotice.firstOrNull {
            it.keys.firstOrNull {
                it.plans.contains(planType.key) && it.periods.contains(period.key)
            } != null
        }?.i18nMessage?.let {
            it.get(language) ?: it.get("en")
        }
    }
}

enum class PaywallScreenStep {
    ONBOARDING_WITH_HISTORY,
    ONBOARDING_NO_HISTORY,
    DEMO_TO_HISTORY,
    DEMO_TO_PAYWALL,
    PERSONAL_LOCATION_HISTORY,
    PAYWALL,
    PURCHASE_CONFIRMATION
}

sealed interface PaywallContinueButtonType {
    val buttonText: String
    val icon: DrawableResource?
    data object Continue: PaywallContinueButtonType {
        override val buttonText: String = MR.strings.paywall_start_button_continue.localizedString()
        override val icon: DrawableResource? = null
    }

    data object ContinueExternal: PaywallContinueButtonType {
        override val buttonText: String = MR.strings.paywall_start_button_continue.localizedString()
        override val icon: DrawableResource = CoreAppIcons.ArrowOpenNew
    }

    data object Ok: PaywallContinueButtonType {
        override val buttonText: String = MR.strings.button_ok.localizedString()
        override val icon: DrawableResource? = null
    }

    data object ManageExternal: PaywallContinueButtonType {
        override val buttonText: String = MR.strings.paywall_start_button_manage.localizedString()
        override val icon: DrawableResource = CoreAppIcons.ArrowOpenNew
    }

    data class FreeTrial(val trialDays: Int): PaywallContinueButtonType {
        override val buttonText: String = MR.strings.paywall_start_button_free.localizedString(
            "$trialDays"
        )
        override val icon: DrawableResource? = null
    }
}

data class FaqSectionState(
    val faqs: List<FaqSectionItem>
)

data class FaqSectionItem(
    val title: AnnotatedString,
    val content: AnnotatedString,
    val expanded: Boolean = false
)

sealed class PaywallScreenEvent() {
    data class OnStartPurchase(
        val planType: PlanType,
        val newPlanProductId: String
    ) : PaywallScreenEvent()

    data object OnRestorePurchase : PaywallScreenEvent()
    data object OpenNativeBillingStorePage : PaywallScreenEvent()
    data class OnNavigateBack(val isSuccess: Boolean) : PaywallScreenEvent()
}

sealed class PaywallScreenUiEvent() {
    data object RefreshPlans : PaywallScreenUiEvent()
    data object StartPurchase : PaywallScreenUiEvent()
    data object RestorePurchase : PaywallScreenUiEvent()
    data class OnPurchaseError(val error: PurchaseErrorException) : PaywallScreenUiEvent()
    data class SelectBillingPeriod(val planType: PlanType, val period: PlanBillingPeriod) :
        PaywallScreenUiEvent()

    data class SelectPlanType(val planType: PlanType) : PaywallScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : PaywallScreenUiEvent()
    data class OfferDialogAction(val action: AlertDialogAction) : PaywallScreenUiEvent()
    data class OnCellItemClick(val table: FeatureTableItemState, val cellItem: FeatureTableItemValue) : PaywallScreenUiEvent()
}

class PurchaseErrorException(
    val errorCode: Int,
    val errorMessage: String,
    val userCancelled: Boolean
) : Throwable(errorMessage)

data class SubscriptionActiveOnAnotherPlatform(
    val planName: String,
    val planProvider: String,
    val event: PaywallScreenUiEvent
) : MainAlertDialog {
    override val key: String = this::class.simpleName ?: "dialog"
    override val dialogData: AlertDialogData = AlertDialogData(
        id = key,
        title = stringResource(MR.strings.plans_management_alert_otherPlatformPlan_title),
        message = stringResource(MR.strings.plans_management_alert_otherPlatformPlan_message, planName, planProvider),
        buttons = listOf(
            AlertDialogButtonData(
                id = Buttons.buttonOk,
                text = stringResource(MR.strings.button_continue),
                color = AlertDialogButtonData.positiveColor,
            ),
            AlertDialogButtonData(
                id = Buttons.buttonCancel,
                text = stringResource(resource = MR.strings.button_cancel)
            )
        )
    )
}