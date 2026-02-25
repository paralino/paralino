package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.settings.billing.SubscriptionScreenEvent
import app.paralino.core.presentation.settings.billing.SubscriptionScreenUiEvent
import app.paralino.core.presentation.settings.billing.SubscriptionViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.settings.SettingsActionItem
import app.paralino.core.ui.settings.SettingsActionItemType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SubscriptionScreenCore(
    appState: CoreUiState,
    vm: SubscriptionViewModel,
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
    openBillingStore: () -> Unit,
    openPaywall: () -> Unit,
    openLicenceScreen: () -> Unit
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                SubscriptionScreenEvent.OnNavigateBack -> {
                    onNavigateBack()
                }

                is SubscriptionScreenEvent.OpenDirectPlanManagementPage -> {
                    uriHandler.openUri(event.url)
                }

                SubscriptionScreenEvent.OpenNativeBillingStorePage -> {
                    openBillingStore()
                }

                null -> {
                    /* do nothing */
                }

            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                SubscriptionScreenUiEvent.DialogAction(
                    AlertDialogAction(
                        dialog = dialog,
                        action = it
                    )
                )
            )
        }
    }

    AnimatedVisibility(visible = uiState.isLoading, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }

    AnimatedVisibility(visible = !uiState.isLoading, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(20.dp)
                .padding(bottom = 40.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.allActivePlans.forEach { plan ->
                    SubscriptionDetailsBox(
                        modifier = Modifier.fillMaxWidth(),
                        isCurrentPlan = plan.isCurrentPlan,
                        planName = plan.planName,
                        billingPeriod = plan.billingPeriod,
                        validUntil = plan.validUntil,
                        planProviderText = plan.planProviderText
                    )
                }
            }

            Spacer(Modifier.size(30.dp))

//            if (uiState.canShowMoreInfo) {
//                Text(text = "Plan: ${uiState.subscriptionState?.name}")
//                Text(text = "Period: ${uiState.subscriptionState?.billingPeriod}")
//                Text(text = "isFromParalino: ${uiState.subscriptionState?.isFromParalino}")
//                Spacer(Modifier.size(20.dp))
//            }

            if (uiState.subscriptionState != null) {
                if (uiState.subscriptionState?.hasPremiumPlan == false) {
                    NotSubscribedSection(openPaywall = openPaywall, openLicenceScreen = openLicenceScreen)
                } else {
                    if (uiState.subscriptionState?.canManagePlan == true) {
                        SettingsActionItem(
                            name = stringResource(MR.strings.plans_management_action_manage),
                            icon = CoreAppIcons.Edit.asPainterResource(),
                            type = if (uiState.subscriptionState?.isManageLoading == true) {
                                SettingsActionItemType.Loading
                            } else {
                                SettingsActionItemType.Link
                            }
                        ) {
                            vm.onEvent(SubscriptionScreenUiEvent.OnManagePlanClick)
                        }

                        SettingsActionItem(
                            name = if (uiState.subscriptionState?.canChangePlanOnDevice == true) {
                                stringResource(MR.strings.plans_management_action_change)
                            } else {
                                stringResource(MR.strings.plans_management_action_explore)
                            },
                            icon = if (uiState.subscriptionState?.canChangePlanOnDevice == true) {
                                CoreAppIcons.Edit.asPainterResource()
                            } else {
                                CoreAppIcons.StarFamily.asPainterResource()
                            },
                            type = SettingsActionItemType.Navigation
                        ) {
                            openPaywall()
                        }

                        SettingsActionItem(
                            name = stringResource(MR.strings.licence_screen_title),
                            icon = CoreAppIcons.LicenseTicket.asPainterResource(),
                            type = SettingsActionItemType.Navigation,
                            onClick = openLicenceScreen
                        )
                    } else {
                        Text(
                            text = stringResource(MR.strings.plans_management_info_wrong_platform_title),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(MR.strings.plans_management_info_wrong_platform_message),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        Spacer(Modifier.size(20.dp))
                        SettingsActionItem(
                            name = stringResource(MR.strings.plans_management_action_explore),
                            icon = CoreAppIcons.StarFamily.asPainterResource(),
                            type = SettingsActionItemType.Navigation
                        ) {
                            openPaywall()
                        }

                        SettingsActionItem(
                            name = stringResource(MR.strings.licence_screen_title),
                            icon = CoreAppIcons.LicenseTicket.asPainterResource(),
                            type = SettingsActionItemType.Navigation,
                            onClick = openLicenceScreen
                        )
                    }
                }
            } else {
                NotSubscribedSection(openPaywall = openPaywall, openLicenceScreen = openLicenceScreen)
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp, top = 40.dp),
                text = stringResource(
                    MR.strings.paywall_current_plan_label,
                    uiState.currentPlanName
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )

            FeatureTable(
                modifier = Modifier,
                table = uiState.featureTableState,
                canExpand = false,
                isExpanded = true,
                onExpandPlans = {},
                onCellClick = { table, cell ->
                    vm.onEvent(
                        SubscriptionScreenUiEvent.OnCellItemClick(
                            table = table,
                            cellItem = cell
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.NotSubscribedSection(
    openPaywall: () -> Unit,
    openLicenceScreen: () -> Unit
) {
    Text(
        text = stringResource(MR.strings.plans_management_info_not_subscribed),
        style = MaterialTheme.typography.h6,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.size(30.dp))
    PremiumPlanBanner(onClick = openPaywall)

    Spacer(Modifier.size(20.dp))
    SettingsActionItem(
        name = stringResource(MR.strings.licence_screen_title),
        icon = CoreAppIcons.LicenseTicket.asPainterResource(),
        type = SettingsActionItemType.Navigation,
        onClick = openLicenceScreen
    )
}

@Composable
private fun SubscriptionDetailsBox(
    modifier: Modifier = Modifier,
    isCurrentPlan: Boolean,
    planName: String,
    billingPeriod: String?,
    validUntil: String?,
    planProviderText: String?
) {
    Box(
        modifier = modifier
            .border(
                width = if (isCurrentPlan) 1.5.dp else 1.dp,
                color = if (isCurrentPlan) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = MaterialTheme.colors.surface.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(
                    if (isCurrentPlan) MR.strings.paywall_current_plan_label else MR.strings.paywall_plan_label,
                    "**$planName**"
                ).annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.fillMaxWidth()
            )

            if (!billingPeriod.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        MR.strings.plans_management_billing_period_label,
                        "**$billingPeriod**"
                    ).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!validUntil.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        MR.strings.plans_management_valid_until_label,
                        "**$validUntil**"
                    ).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!planProviderText.isNullOrBlank()) {
                Spacer(Modifier.size(4.dp))
                Divider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color.Companion.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.size(4.dp))
                CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                    Text(
                        text = planProviderText.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}