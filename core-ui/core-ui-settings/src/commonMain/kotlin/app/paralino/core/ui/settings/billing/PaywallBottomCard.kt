package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanType
import app.paralino.core.presentation.settings.billing.PaywallScreenState
import app.paralino.core.presentation.settings.billing.PaywallScreenStep
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun BoxScope.PaywallBottomCard(
    modifier: Modifier = Modifier,
    uiState: PaywallScreenState,
    paddingValues: PaddingValues,
    onHeightChange: (Int) -> Unit,
    onEvent: (PaywallScreenUiEvent) -> Unit
) {
    var bottomCardHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    var buttonVisible by remember { mutableStateOf(true) }
    //        LaunchedEffect(uiState.screenStep) {
//            when (uiState.screenStep) {
//                PaywallScreenStep.ONBOARDING -> {
//                    buttonVisible = false
//                    delay(1200)
//                    buttonVisible = true
//                }
//
//                else -> {
//                    buttonVisible = true
//                }
//            }
//        }

    AnimatedVisibility(
        modifier = modifier.align(Alignment.BottomCenter),
        visible = (uiState.selectedPlan != null || uiState.screenStep != PaywallScreenStep.PAYWALL),
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { bottomCardHeight.toDp() })
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colors.background,
                            MaterialTheme.colors.background
                        )
                    )
                )
        )

        PaywallBottomCard(
            state = uiState,
            paddingValues = paddingValues,
            isButtonVisible = buttonVisible,
            onStartPurchase = { onEvent(PaywallScreenUiEvent.StartPurchase) },
            onPeriodSelected = { planType, period ->
                onEvent(PaywallScreenUiEvent.SelectBillingPeriod(planType = planType, period = period))
            },
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    bottomCardHeight = coordinates.size.height
                    onHeightChange(bottomCardHeight)
                }
        )
    }
}

@Composable
private fun PaywallBottomCard(
    modifier: Modifier = Modifier,
    state: PaywallScreenState,
    isButtonVisible: Boolean,
    paddingValues: PaddingValues,
    onStartPurchase: () -> Unit,
    onPeriodSelected: (PlanType, PlanBillingPeriod) -> Unit,
) {
    val billingPeriodState = state.selectedBillingPeriod
    val contentColor = MaterialTheme.colors.onBackground
    val selectedPlan by rememberUpdatedState(state.selectedPlan)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .dropShadow(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadow = Shadow(
                    radius = 16.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                )
            )
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 12.dp)
                .padding(bottom = paddingValues.calculateBottomPadding())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val periodPlan = selectedPlan
            AnimatedVisibility(
                periodPlan != null && state.canShowBottomPlanPeriodSelector
            ) {
                if (periodPlan != null) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        BillingPeriodOptionsSelector(
                            modifier = Modifier,
                            selectedPlan = periodPlan,
                            showTrialInfo = !state.hasActivePlanOnAnotherPlatform,
                            onPeriodSelected = { onPeriodSelected(periodPlan.type, it) }
                        )
                    }
                }
            }

            AnimatedVisibility(state.hasActivePlanOnAnotherPlatform && state.screenStep == PaywallScreenStep.PAYWALL) {
                SmallActionButton(
                    modifier = Modifier.padding(bottom = 8.dp),
                    backgroundColor = Color.Transparent,
                    contentColor = animateColorAsState(if (isSystemInDarkTheme()) AppColors.highlightGold else AppColors.redMain).value,
                    icon = CoreAppIcons.Warning.asPainterResource(),
                    compactMode = true,
                    iconSize = 16.dp,
                    text = {
                        Text(
                            text = stringResource(MR.strings.plans_management_info_otherPlatformPlan_message),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.caption
                        )
                    },
                    onClick = null
                )
            }

            AnimatedVisibility(state.showExternalPricingInfo) {
                NoticeInfoChip(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(unbounded = true)
                        .padding(bottom = 12.dp),
                    text = stringResource(MR.strings.plans_management_info_externalPricing_message),
                    icon = CoreAppIcons.InfoCircle.asPainterResource()
                )
            }

            AnimatedVisibility(state.screenStep != PaywallScreenStep.PAYWALL) {
                Spacer(modifier = Modifier.size(6.dp))
            }

            AnimatedVisibility(
                !state.showExternalPricingInfo && periodPlan != null && state.canShowBottomPlanPeriodSelector
            ) {
                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    color = Color.Gray.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
            }

            AnimatedVisibility(selectedPlan != null || state.screenStep != PaywallScreenStep.PAYWALL) {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG,
                    state = AppButtonState.loadingState(!isButtonVisible || state.isPurchaseButtonLoading),
                    enabled = isButtonVisible,
                    onClick = {
                        if (isButtonVisible) {
                            onStartPurchase()
                        }
                    }
                ) {
                    Text(
                        text = state.continueButtonType.buttonText.annotatedBoldCompose(),
                        style = MaterialTheme.typography.button.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    if (state.continueButtonType.icon != null) {
                        Icon(
                            painter = state.continueButtonType.icon!!.asPainterResource(),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(selectedPlan != null && state.screenStep == PaywallScreenStep.PAYWALL && !state.hasActivePlanOnAnotherPlatform && !state.isLoading) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = if (billingPeriodState?.trialDurationDays != null) {
                        stringResource(MR.strings.paywall_cancellation_notice_trial)
                    } else {
                        stringResource(MR.strings.paywall_cancellation_notice)
                    },
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    color = contentColor.copy(alpha = ContentAlpha.medium)
                )
            }

            AnimatedVisibility(selectedPlan == null && state.screenStep == PaywallScreenStep.PAYWALL) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    text = stringResource(MR.strings.paywall_select_plan_label),
                    style = MaterialTheme.typography.h5,
                    textAlign = TextAlign.Center,
                    color = contentColor
                )
            }
        }
    }
}