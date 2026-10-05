package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.settings.billing.PaywallScreenStep
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal fun DemoToHistoryStep(
    modifier: Modifier = Modifier,
    memberName: String,
    memberImage: ImageModel,
    deviceName: String,
    screenStep: PaywallScreenStep,
    valueFormatter: IValueFormatter,
    paddingValues: PaddingValues
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = screenStep == PaywallScreenStep.DEMO_TO_HISTORY || screenStep == PaywallScreenStep.DEMO_TO_PAYWALL,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .background(
                    color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(bottom = 140.dp)
                .padding(top = 30.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                text = stringResource(MR.strings.paywall_header_feature_demo_screen),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))

            PaywallScreenFeatureElements(
                modifier = Modifier
                    .padding(bottom = 20.dp, top = 20.dp)
                    .fillMaxWidth(),
                displayTitle = false,
                displayLocationHistory = screenStep == PaywallScreenStep.DEMO_TO_PAYWALL,
                memberName = memberName,
                memberImageModel = memberImage,
                memberDeviceName = deviceName,
                valueFormatter = valueFormatter
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun OnboardingShowcaseStep(
    modifier: Modifier = Modifier,
    screenStep: PaywallScreenStep,
    screenType: BillingScreenSourceType,
    paddingValues: PaddingValues
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = screenStep == PaywallScreenStep.ONBOARDING_WITH_HISTORY || screenStep == PaywallScreenStep.ONBOARDING_NO_HISTORY,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(bottom = 100.dp)
                .padding(top = 30.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                text = if (screenType == BillingScreenSourceType.GROUP_ONBOARDING) {
                    stringResource(MR.strings.paywall_header_feature_list_screen_family)
                } else {
                    stringResource(MR.strings.paywall_header_feature_list_screen_solo)
                },
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            PaywallFeatureBulletPointSection(modifier = Modifier.padding(vertical = 20.dp))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
internal fun PersonalHistoryStep(
    modifier: Modifier = Modifier,
    screenStep: PaywallScreenStep,
    message: String,
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = screenStep == PaywallScreenStep.PERSONAL_LOCATION_HISTORY,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        PaywallScreenFeatureLocationHistory(
            modifier = Modifier
                .fillMaxSize(),
            paddingValues = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 100.dp
            ),
            content = message
        )
    }
}

@Composable
internal fun PurchaseConfirmationStep(
    modifier: Modifier = Modifier,
    screenStep: PaywallScreenStep
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = screenStep == PaywallScreenStep.PURCHASE_CONFIRMATION,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        PaywallScreenPurchaseConfirmation(
            modifier = Modifier.fillMaxSize(),
            paddingValues = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 100.dp
            )
        )
    }
}

@Composable
internal fun FullScreenLoadingStep(
    modifier: Modifier = Modifier,
    screenStep: PaywallScreenStep,
    isLoading: Boolean
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = isLoading && screenStep == PaywallScreenStep.PAYWALL,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }
}