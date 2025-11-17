package app.paralino.core.presentation.settings.billing

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.base.SubscriptionInfoNotice
import app.paralino.core.provider.current
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.billing.BillingScreenSourceType

data class PaywallScreenState(
    val freePlanInfo: PlanInfoState,
    val plans: List<PlanInfoState>,
    val canShowMoreInfo: Boolean,
    val isLoading: Boolean,
    val isPurchaseButtonLoading: Boolean,
    val alertDialog: MainAlertDialog?,
    val faq: FaqSectionState?,
    val isPromoCodeSectionVisible: Boolean,
    val promoCode: String,
    val screenStep: PaywallScreenStep,
    val screenType: BillingScreenSourceType,
    val memberImageModel: ImageModel,
    val memberName: String,
    val memberDeviceName: String,
    val locationHistoryStepMessage: String,
    val subscriptionNotice: List<SubscriptionInfoNotice>,
    val subscriptionOfferDialog: MainAlertDialog.SubscriptionOfferDialog?
) {

    val currentPlan: PlanInfoState? = plans.firstOrNull { it.isCurrentPlan }
    val currentPlanName: String = currentPlan?.let { "**${it.name}**" } ?: ""
    val canMakePurchaseOnThisPlatform: Boolean = when (currentPlan?.currentPlanProvider) {
        PlanProvider.APPLE_APP_STORE -> DeviceType.current == DeviceType.iOS
        PlanProvider.GOOGLE_PLAY_STORE -> DeviceType.current == DeviceType.Android
        PlanProvider.PARALINO -> false
        null -> true
    }

    val selectedPlan: PlanInfoState? = plans.firstOrNull { it.isSelected }
    val selectedBillingPeriod: PlanBillingPeriodState? = selectedPlan?.selectedBillingPeriod?.takeIf {
        canMakePurchaseOnThisPlatform
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
        if (plan.selectedBillingPeriod != null) {
            noticeForPlan(planType = plan.config.planTypeEnum, period = plan.selectedBillingPeriod.billingPeriod)
        } else {
            null
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

data class PlanInfoState(
    val productId: String,
    val name: String,
    val config: PlanConfig,
    val isCurrentPlan: Boolean,
    val currentPlanProvider: PlanProvider?,
    val hasSpecialOffer: Boolean,
    val isSelected: Boolean,
    val isExpanded: Boolean,
    val billingPeriods: List<PlanBillingPeriodState>
) {
    val type: PlanType = config.planTypeEnum
    val selectedBillingPeriod: PlanBillingPeriodState? =
        billingPeriods.firstOrNull { it.isSelected }

    val displayedName: String = if (hasSpecialOffer) {
        "🎁$name"
    } else {
        name
    }
}

data class PlanBillingPeriodState(
    val productPlanId: String,
    val billingPeriod: PlanBillingPeriod,
    val name: String,
    val isSelected: Boolean,
    val price: String,
    val pricePerMonth: String?,
    val pricePerMember: String?,
    val discountedPrice: String?,
    val discountPercent: String?,
    val isPromoDiscount: Boolean,
    val trialDurationDays: Int?,
    val isCurrentBillingPeriod: Boolean
)

enum class PaywallScreenStep {
    ONBOARDING, DEMO, LOCATION_HISTORY, PAYWALL
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
    data class OnNavigateBack(val isSuccess: Boolean) : PaywallScreenEvent()
}

sealed class PaywallScreenUiEvent() {
    data object RefreshPlans : PaywallScreenUiEvent()
    data object StartPurchase : PaywallScreenUiEvent()
    data object RestorePurchase : PaywallScreenUiEvent()
    data object RequestPromoCode : PaywallScreenUiEvent()
    data object ApplyPromoCode : PaywallScreenUiEvent()
    data class OnChangePromoCode(val code: String) : PaywallScreenUiEvent()
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