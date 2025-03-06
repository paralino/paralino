package app.paralino.core.presentation.settings.billing

import androidx.compose.ui.text.AnnotatedString
import app.paralino.core.MR
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanType
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class PaywallScreenState(
    val freePlanInfo: PlanInfoState,
    val plans: List<PlanInfoState>,
    val canShowMoreInfo: Boolean,
    val isLoading: Boolean,
    val isPurchaseButtonLoading: Boolean,
    val alertDialog: MainAlertDialog?,
    val faq: FaqSectionState?,
    val isPromoCodeSectionVisible: Boolean,
    val promoCode: String
) {
    val selectedPlan: PlanInfoState? = plans.firstOrNull { it.isSelected }
    val selectedBillingPeriod: PlanBillingPeriodState? = selectedPlan?.selectedBillingPeriod

    val featureTable: FeatureTableState =
        constructFeaturesTable(freeUiPlan = freePlanInfo, uiPlans = plans)
    val featureTableForSelectedPlan: FeatureTableState? = selectedPlan?.let { plan ->
        FeatureTableState(
            features = featureTable.features.map {
                it.copy(
                    values = it.values.mapNotNull {
                        if (it.planType == plan.type || !it.planType.isPremium) {
                            it
                        } else {
                            null
                        }
                    }
                )
            }
        )
    }

    private fun constructFeaturesTable(
        freeUiPlan: PlanInfoState,
        uiPlans: List<PlanInfoState>
    ): FeatureTableState {
        val plans = buildList {
            add(freeUiPlan)
            addAll(uiPlans)
        }

        val values = buildList {
            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_table_header.localizedString(),
                    isTitle = true,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = it.name,
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.TOP
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_privacy.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = "✅",
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_groups.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = it.config.groupsLimit.toString(),
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_members.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = it.config.membersLimit.toString(),
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_devices.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = it.config.devicesLimit.toString(),
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_places.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = it.config.placesLimit.toString(),
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_battery.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = "✅",
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_locate.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = if (it.type.isPremium) "✅" else "❌",
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_performance.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = if (it.type.isSilverOrMore) "✅" else "❌",
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_multi_device.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = if (it.config.devicesLimit > 1) "✅" else "❌",
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.FULL
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )

            add(
                FeatureTableItemState(
                    name = MR.strings.paywall_feature_notify.localizedString(),
                    isTitle = false,
                    values = plans.map {
                        FeatureTableItemValue(
                            name = if (it.type.isSilverOrMore) "✅" else "❌",
                            planType = it.type,
                            highlightMode = if (it.isSelected) {
                                FeatureTableItemHighlightMode.BOTTOM
                            } else {
                                FeatureTableItemHighlightMode.NONE
                            }
                        )
                    }
                )
            )
        }

        return FeatureTableState(features = values)
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
    val includedMembersPerGroup: Int,
    val billingPeriods: List<PlanBillingPeriodState>,
    val benefits: List<PlanConfig.PlanBenefit>
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

data class FeatureTableState(
    val features: List<FeatureTableItemState>
)

data class FeatureTableItemState(
    val name: String,
    val isTitle: Boolean,
    val values: List<FeatureTableItemValue>
)

data class FeatureTableItemValue(
    val name: String,
    val planType: PlanType,
    val highlightMode: FeatureTableItemHighlightMode,
) {
    val isPremium: Boolean = planType.isPremium
}

enum class FeatureTableItemHighlightMode {
    NONE, FULL, TOP, BOTTOM
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