package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.settings.billing.SubscriptionDetailsState
import app.paralino.core.presentation.settings.billing.SubscriptionScreenEvent
import app.paralino.core.presentation.settings.billing.SubscriptionScreenUiEvent
import app.paralino.core.presentation.settings.billing.SubscriptionViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.SettingsActionItem
import app.paralino.core.presentation.settings.SettingsActionItemType
import app.paralino.core.ui.components.MembershipPlanFeatureTable
import app.paralino.core.ui.components.PremiumPlanBanner
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.allActivePlans.forEach { plan ->
                    SubscriptionDetailsBox(
                        modifier = Modifier.fillMaxWidth(),
                        state = plan,
                        onManagePlanClick = {
                            vm.onEvent(SubscriptionScreenUiEvent.OnManagePlanClick(planId = plan.planId))
                        }
                    )
                }
            }

            if (!uiState.hasPremiumPlan) {
                NotSubscribedSection(openPaywall = openPaywall)
            } else {
                Spacer(Modifier.size(20.dp))
                SettingsActionItem(
                    name = if (uiState.canChangePlanOnThisDevice) {
                        stringResource(MR.strings.plans_management_action_change)
                    } else {
                        stringResource(MR.strings.plans_management_action_explore)
                    },
                    icon = if (uiState.canChangePlanOnThisDevice) {
                        CoreAppIcons.Edit.asPainterResource()
                    } else {
                        CoreAppIcons.StarFamily.asPainterResource()
                    },
                    type = SettingsActionItemType.Navigation,
                    onClick = openPaywall
                )
            }

            SettingsActionItem(
                name = stringResource(MR.strings.licence_screen_title),
                icon = CoreAppIcons.LicenseTicket.asPainterResource(),
                type = SettingsActionItemType.Navigation,
                onClick = openLicenceScreen
            )

            Spacer(Modifier.size(40.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp),
                text = if (uiState.hasPremiumPlan) {
                    stringResource(MR.strings.plans_management_activePlan_tag)
                } else {
                    stringResource(
                        MR.strings.paywall_current_plan_label,
                        uiState.currentPlanName
                    )
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )

            MembershipPlanFeatureTable(
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
    openPaywall: () -> Unit
) {
    ConditionErrorItem(
        modifier = Modifier.fillMaxWidth(),
        icon = CoreAppIcons.StarShine.asPainterResource(),
        text = stringResource(MR.strings.plans_management_info_not_subscribed),
        endAction = null
    )

    Spacer(Modifier.size(20.dp))
    PremiumPlanBanner(onClick = openPaywall)
    Spacer(Modifier.size(30.dp))
}

@Composable
private fun SubscriptionDetailsBox(
    modifier: Modifier = Modifier,
    state: SubscriptionDetailsState,
    onManagePlanClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        var topPadding by remember { mutableStateOf(if (state.isCurrentPlan) 10.dp else 0.dp) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .conditional(state.isCurrentPlan) {
                    padding(top = animateDpAsState(topPadding).value)
                }
                .border(
                    width = if (state.isCurrentPlan) 1.5.dp else 1.dp,
                    color = if (state.isCurrentPlan) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                    shape = MaterialTheme.shapes.medium
                )
                .background(
                    color = MaterialTheme.colors.surface.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(
                    if (state.isCurrentPlan) MR.strings.paywall_current_plan_label else MR.strings.paywall_plan_label,
                    "**${state.planName}**"
                ).annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.fillMaxWidth()
            )

            if (!state.billingPeriod.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        MR.strings.plans_management_billingPeriod_label,
                        "**${state.billingPeriod}**"
                    ).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.nextRenewal.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        MR.strings.plans_management_nextRenewal_label,
                        "**${state.nextRenewal}**"
                    ).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.expiredAt.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        MR.strings.plans_management_expiresAt_label,
                        "**${state.expiredAt}**"
                    ).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    color = AppColors.redMain,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.expiresAt.isNullOrBlank()) {
                Text(
                    text = if (state.expiredAt.isNullOrBlank()) {
                        stringResource(
                            MR.strings.plans_management_expiresAt_label,
                            "**${state.expiresAt}**"
                        ).annotatedBoldCompose()
                    } else {
                        stringResource(
                            MR.strings.plans_management_expired_label,
                            "**${state.expiresAt}**"
                        ).annotatedBoldCompose()
                    },
                    style = MaterialTheme.typography.caption,
                    color = if (state.expiredAt.isNullOrBlank()) MaterialTheme.colors.onSurface else AppColors.redMain,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!state.planProviderText.isNullOrBlank()) {
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
                        text = (state.planProviderText ?: "").annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (state.canManagePlanOnThisDevice == true) {
                SmallActionButton(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.End),
                    icon = CoreAppIcons.Edit.asPainterResource(),
                    text = {
                        Text(
                            modifier = Modifier.padding(end = 2.dp)
                                .weight(1f, fill = false),
                            text = stringResource(MR.strings.plans_management_action_manage)
                        )
                    },
                    backgroundColor = if (isSystemInDarkTheme()) {
                        AppColors.accentLight.copy(alpha = 0.2f)
                    } else {
                        AppColors.accent.copy(alpha = 0.2f)
                    },
                    contentColor = MaterialTheme.colors.onBackground,
                    isLoading = state.isManageLoading,
                    hasEndArrow = true,
                    onClick = onManagePlanClick
                )
            } else if (state.canManagePlanOnThisDevice == false) {
                Spacer(Modifier.size(4.dp))
                CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                    Text(
                        text = stringResource(MR.strings.plans_management_info_wrong_platform_title),
                        style = MaterialTheme.typography.caption.copy(color = AppColors.redMain),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (state.isCurrentPlan) {
            val density = LocalDensity.current
            SmallActionButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp)
                    .onGloballyPositioned {
                        topPadding = with(density) { it.size.height.toDp() / 2 }
                    },
                icon = null,
                text = {
                    Text(
                        modifier = Modifier
                            .padding(end = 2.dp),
                        text = stringResource(MR.strings.plans_management_activePlan_tag),
                        style = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                backgroundColor = AppColors.highlightGold,
                contentColor = AppColors.darkBackground,
                compactMode = true,
                isLoading = false,
                hasEndArrow = false,
                onClick = null
            )
        }
    }
}