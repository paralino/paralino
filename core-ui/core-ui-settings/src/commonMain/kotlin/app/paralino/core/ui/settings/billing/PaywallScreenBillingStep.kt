package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.presentation.settings.SettingsActionItemType
import app.paralino.core.presentation.settings.billing.PaywallScreenState
import app.paralino.core.presentation.settings.billing.PaywallScreenStep
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.MembershipPlanFeatureTable
import app.paralino.core.ui.components.MembershipPlanSummaryTable
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource


@Composable
internal fun PaywallScreenBillingStep(
    modifier: Modifier = Modifier,
    uiState: PaywallScreenState,
    paddingValues: PaddingValues,
    bottomCardHeight: Int,
    valueFormatter: IValueFormatter,
    onNavigateBack: (Boolean) -> Unit,
    openLicenceScreen: () -> Unit,
    onEvent: (PaywallScreenUiEvent) -> Unit
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = !uiState.isLoading && uiState.screenStep == PaywallScreenStep.PAYWALL,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
    ) {
        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top
        ) {
            DefaultToolbar(
                title = if (uiState.isLoading || uiState.selectedPlan == null) {
                    null
                } else {
                    stringResource(MR.strings.paywall_screen_title)
                },
                navigationIcon = if (uiState.isLoading || !uiState.canCloseScreen) {
                    null
                } else {
                    { ToolbarButtons.Close { onNavigateBack(false) } }
                },
                elevation = 0.dp
            )

            PaywallScreenBillingStep(
                valueFormatter = valueFormatter,
                state = uiState,
                bottomCardHeight = bottomCardHeight,
                onTermsClick = { uriHandler.openUri(BackendResourceIDs.termsOfUseUrl) },
                onPrivacyClick = { uriHandler.openUri(BackendResourceIDs.privacyPolicyUrl) },
                onEvent = { onEvent(it) },
                openLicenceScreen = openLicenceScreen
            )
        }
    }
}

@Composable
private fun ColumnScope.PaywallScreenBillingStep(
    valueFormatter: IValueFormatter,
    state: PaywallScreenState,
    bottomCardHeight: Int,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onEvent: (PaywallScreenUiEvent) -> Unit,
    openLicenceScreen: () -> Unit
) {
    val density = LocalDensity.current

    AnimatedVisibility(state.selectedPlan == null) {
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            text = stringResource(MR.strings.paywall_screen_header),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
    }

    AnimatedVisibility(
        state.currentPlan != null
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp, top = 8.dp),
            text = stringResource(MR.strings.paywall_current_plan_label, state.currentPlanName).annotatedBoldCompose(),
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
        )
    }

    AnimatedVisibility(
        state.currentPlan == null
    ) {
        Spacer(modifier = Modifier.size(16.dp))
    }

    PlanTypePickerTabs(
        state = state,
        onEvent = onEvent
    )

    val selectedPlan = state.selectedPlan
    Spacer(modifier = Modifier.size(16.dp))

    AnimatedVisibility(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        visible = !state.hasActivePlanOnAnotherPlatform && state.subscriptionNoticeForCurrentPlan != null
    ) {
        NoticeInfoChip(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            text = state.subscriptionNoticeForCurrentPlan ?: "",
            icon = {
                Icon(
                    painter = CoreAppIcons.StarFamily.asPainterResource(),
                    contentDescription = null
                )
            }
        )
    }

    var benefitsMode by remember { mutableStateOf(PlanFeatureTableMode.SUMMARY) }
    val summaryTable = state.featureSummaryForSelectedPlan

    AnimatedVisibility(summaryTable != null && selectedPlan != null && benefitsMode == PlanFeatureTableMode.SUMMARY) {
        if (summaryTable != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                MembershipPlanSummaryTable(
                    content = summaryTable,
                    onExpandClick = {
                        benefitsMode = PlanFeatureTableMode.SELECTED_PLAN
                    }
                )
            }
        }
    }

    AnimatedVisibility(selectedPlan == null || benefitsMode != PlanFeatureTableMode.SUMMARY) {
        val isTableExpanded = benefitsMode != PlanFeatureTableMode.SUMMARY
        MembershipPlanFeatureTable(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = animateDpAsState(if (isTableExpanded) 10.dp else 0.dp).value)
                .padding(horizontal = 20.dp),
            table = if (benefitsMode == PlanFeatureTableMode.ALL_PLANS) {
                state.fullFeatureTable
            } else {
                state.featureTableForSelectedPlan ?: state.fullFeatureTable
            },
            isExpanded = isTableExpanded,
            onExpandPlans = {
                benefitsMode = PlanFeatureTableMode.ALL_PLANS
            },
            onCellClick = { featureTableState, item ->
                onEvent(
                    PaywallScreenUiEvent.OnCellItemClick(
                        table = featureTableState,
                        cellItem = item
                    )
                )
            }
        )
    }

    // when table is expanded, also expand and show all features as well
    if (!state.screenType.isOnboarding || benefitsMode != PlanFeatureTableMode.SUMMARY) {
        PaywallScreenFeatureElements(
            modifier = Modifier
                .padding(top = 30.dp)
                .background(MaterialTheme.colors.background)
                .background(
                    color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                )
                .padding(bottom = 40.dp, top = 20.dp)
                .fillMaxWidth(),
            displayTitle = true,
            displayLocationHistory = true,
            memberName = state.memberName,
            memberImageModel = state.memberImageModel,
            memberDeviceName = state.memberDeviceName,
            valueFormatter = valueFormatter
        )

        PaywallFeatureBulletPointSection(modifier = Modifier.padding(top = 20.dp))
    }

    if (!state.screenType.isOnboarding) {
        Spacer(Modifier.size(40.dp))
    } else {
        Spacer(Modifier.size(30.dp))
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 4.dp),
        text = stringResource(MR.strings.paywall_header_user_reviews),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
    )

    PaywallUserReviewsSection()

    if (state.canShowAdditionalPlanSelector) {
        Column(
            modifier = Modifier
                .padding(top = 40.dp)
                .padding(bottom = 20.dp)
                .background(
                    color = AppColors.accent.copy(alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f),
                )
                .padding(top = 50.dp, bottom = 45.dp)
        ) {
            PlanTypePickerTabs(
                state = state,
                onEvent = onEvent
            )

            if (summaryTable != null) {
                MembershipPlanSummaryTable(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp),
                    content = summaryTable,
                    onExpandClick = null
                )
            }
        }
    } else {
        Spacer(Modifier.size(30.dp))
    }

    state.faq?.let { faq ->
        PaywallFaqSection(
            modifier = Modifier.padding(horizontal = 20.dp),
            faqsList = faq.faqs
        )
    }

    Spacer(modifier = Modifier.size(12.dp))

    SettingsActionItem(
        modifier = Modifier.padding(horizontal = 20.dp),
        name = stringResource(MR.strings.setting_title_privacy),
        icon = CoreAppIcons.PrivacyTip.asPainterResource(),
        type = SettingsActionItemType.Link,
        onClick = onPrivacyClick
    )

    SettingsActionItem(
        modifier = Modifier.padding(horizontal = 20.dp),
        name = stringResource(MR.strings.setting_title_terms),
        icon = CoreAppIcons.PrivacyTip.asPainterResource(),
        type = SettingsActionItemType.Link,
        onClick = onTermsClick
    )


    if (state.canRestorePurchases) {
        SettingsActionItem(
            modifier = Modifier.padding(horizontal = 20.dp),
            name = stringResource(MR.strings.paywall_button_restore),
            icon = CoreAppIcons.Sync.asPainterResource(),
            type = SettingsActionItemType.Action,
            onClick = { onEvent(PaywallScreenUiEvent.RestorePurchase) }
        )
    }

    SettingsActionItem(
        modifier = Modifier.padding(horizontal = 20.dp),
        name = stringResource(MR.strings.licence_screen_title),
        icon = CoreAppIcons.LicenseTicket.asPainterResource(),
        type = SettingsActionItemType.Navigation,
        onClick = openLicenceScreen
    )

    Spacer(modifier = Modifier.size(8.dp))
    Spacer(modifier = Modifier.size(with(density) { bottomCardHeight.toDp() }))
}

private enum class PlanFeatureTableMode {
    SUMMARY,
    SELECTED_PLAN,
    ALL_PLANS
}