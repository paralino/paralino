package app.paralino.core.presentation.settings.billing

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanType
import app.paralino.core.provider.localizedString
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
) {
    val selectedPlan: PlanInfoState? = plans.firstOrNull { it.isSelected }
    val selectedBillingPeriod: PlanBillingPeriodState? = selectedPlan?.selectedBillingPeriod

    val featureTable: FeatureTableState =
        FeatureTableState.constructFeaturesTable(freeUiPlan = freePlanInfo, uiPlans = plans)
    val featureTableForSelectedPlan: FeatureTableState? = selectedPlan?.let { plan ->
        FeatureTableState(
            canExpand = true,
            features = featureTable.features.map {
                it.copy(
                    values = it.values.mapNotNull {
                        if (it.planType == plan.type) {
                            it
                        } else {
                            null
                        }
                    }
                )
            }
        )
    }
}

data class PlanInfoState(
    val productId: String,
    val name: String,
    val config: PlanConfig,
    val isCurrentPlan: Boolean,
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
    } + if (isCurrentPlan) "*" else ""
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
)

enum class PaywallScreenStep {
    ONBOARDING, DEMO, PAYWALL
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
}

class PurchaseErrorException(
    val errorCode: Int,
    val errorMessage: String,
    val userCancelled: Boolean
) : Throwable(errorMessage)