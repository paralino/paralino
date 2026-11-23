package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.presentation.billing.IntroOfferState
import app.paralino.core.presentation.billing.PlanInfoState
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal fun BillingPeriodOptionsSelector(
    modifier: Modifier = Modifier,
    selectedPlan: PlanInfoState,
    showTrialInfo: Boolean,
    onPeriodSelected: (PlanBillingPeriod) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedPlan.billingPeriods.forEach { state ->
                val trialDays = state.trialDurationDays?.takeIf { showTrialInfo }
                SubscriptionOptionItem(
                    title = state.name,
                    trialInfo = when {
                        // indicate trial so it is more clear which periods have it
                        trialDays != null -> stringResource(
                            MR.strings.paywall_free_trial_notice_billingPeriod,
                            "$trialDays"
                        )

                        else -> null
                    },
                    hasTrial = trialDays != null,
                    introOfferInfo = state.introOffer?.let { introOfferText(it) },
                    discount = state.discount,
                    price = state.price,
                    priceUnit = when (state.billingPeriod) {
                        PlanBillingPeriod.MONTHLY -> stringResource(MR.strings.paywall_billing_unit_month)
                        PlanBillingPeriod.ANNUALLY -> stringResource(MR.strings.paywall_billing_unit_year)
                        else -> null
                    },
                    monthlyEquivalent = state.pricePerMonth,
                    isSelected = state.isSelected,
                    isCurrent = state.isNativePlatformCurrentlyActiveBillingPeriod,
                    onClick = { onPeriodSelected(state.billingPeriod) }
                )
            }
        }

        AnimatedVisibility(
            visible = selectedPlan.selectedBillingPeriod?.pricePerMember != null && selectedPlan.selectedBillingPeriod?.isNativePlatformCurrentlyActiveBillingPeriod == false
        ) {
            Text(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                text = stringResource(
                    MR.strings.paywall_price_per_member,
                    selectedPlan.selectedBillingPeriod?.pricePerMember ?: "",
                    selectedPlan.config.membersLimit
                ).annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                color = LocalContentColor.current.copy(alpha = ContentAlpha.medium),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun introOfferText(offer: IntroOfferState): String {
    val months = offer.durationMonths
    val duration = when {
        months == 1 -> stringResource(MR.strings.paywall_intro_offer_first_month)
        months >= 24 && months % 12 == 0 -> stringResource(MR.plurals.paywall_intro_offer_first_years, months / 12, months / 12)
        else -> stringResource(MR.plurals.paywall_intro_offer_first_months, months, months)
    }
    // only month or year
    val priceUnit = when (offer.recurringPeriodMonths) {
        1 -> stringResource(MR.strings.paywall_billing_unit_month)
        12 -> stringResource(MR.strings.paywall_billing_unit_year)
        else -> ""
    }
    return stringResource(MR.strings.paywall_intro_offer, offer.price + priceUnit, duration)
}

@Composable
private fun SubscriptionOptionItem(
    title: String,
    trialInfo: String?,
    hasTrial: Boolean,
    introOfferInfo: String?,
    discount: String?,
    price: String,
    priceUnit: String?,
    monthlyEquivalent: String?,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = LocalContentColor.current
    val selectedColor = if (isDark) AppColors.accentDark else MaterialTheme.colors.primary
    val accentColor = if (isDark) AppColors.accent else AppColors.accentDark

    val backgroundColor by animateColorAsState(
        if (!isSelected) MaterialTheme.colors.background else MaterialTheme.colors.surface
    )
    val borderColor by animateColorAsState(
        if (isSelected) selectedColor else Color.Gray.copy(alpha = 0.2f)
    )
    val radioColor by animateColorAsState(
        if (isSelected) selectedColor else contentColor.copy(alpha = ContentAlpha.disabled)
    )


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(width = 2.dp, color = radioColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(radioColor, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 20.sp,
                        color = contentColor
                    )

                    AnimatedVisibility(discount != null && !isCurrent) {
                        Text(
                            modifier = Modifier
                                .background(
                                    color = AppColors.redMain,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            text = stringResource(
                                MR.strings.paywall_discount_percentage_badge_text,
                                discount ?: ""
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = AppColors.lightBackground
                        )
                    }
                }

                AnimatedVisibility(isCurrent) {
                    Text(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        text = stringResource(MR.strings.paywall_active_planBillingPeriod_label),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colors.onPrimary
                    )
                }

                AnimatedVisibility(trialInfo != null && introOfferInfo == null && !isCurrent) {
                    Text(
                        text = (trialInfo ?: "").annotatedBoldCompose(),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasTrial) accentColor else contentColor.copy(alpha = ContentAlpha.medium)
                    )
                }
            }

            AnimatedVisibility(!isCurrent) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = price,
                            color = contentColor,
                            style = MaterialTheme.typography.subtitle1.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                lineHeight = 20.sp
                            )
                        )
                        if (priceUnit != null) {
                            Text(
                                text = priceUnit,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = contentColor.copy(alpha = ContentAlpha.medium)
                            )
                        }
                    }

                    if (monthlyEquivalent != null) {
                        Text(
                            text = monthlyEquivalent.annotatedBoldCompose(),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = contentColor.copy(alpha = ContentAlpha.medium)
                        )
                    }
                }
            }
        }

        AnimatedVisibility((introOfferInfo != null) && !isCurrent) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Divider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp),
                    color = Color.Gray.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏷",
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = contentColor
                    )

                    Column(
                        modifier = Modifier,
                    ) {
                        AnimatedVisibility(trialInfo != null) {
                            Text(
                                text = (trialInfo ?: "").annotatedBoldCompose(),
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor.copy(alpha = 0.85f)
                            )
                        }

                        AnimatedVisibility(introOfferInfo != null && trialInfo != null) {
                            Spacer(modifier = Modifier.size(3.dp))
                        }

                        AnimatedVisibility(introOfferInfo != null) {
                            Text(
                                text = (introOfferInfo ?: "").annotatedBoldCompose(),
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = contentColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}