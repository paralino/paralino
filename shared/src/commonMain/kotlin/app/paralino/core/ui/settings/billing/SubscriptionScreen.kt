package app.paralino.core.ui.settings.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SubscriptionScreenCore(
    appState: CoreUiState,
    vm: SubscriptionViewModel,
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
    openBillingStore: () -> Unit,
    openBillingManagementScreen: () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                SubscriptionScreenEvent.OnNavigateBack -> {
                    onNavigateBack()
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
                .padding(paddingValues)
                .padding(20.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {

            if (uiState.canShowMoreInfo) {
                Text(text = "Plan: ${uiState.subscriptionState?.name}")
                Text(text = "ExpiresAt: ${uiState.subscriptionState?.expiresAt}")
                Text(text = "Period: ${uiState.subscriptionState?.billingPeriod}")
                Text(text = "isFromParalino: ${uiState.subscriptionState?.isFromParalino}")
                Spacer(Modifier.size(40.dp))
            }

            if (uiState.subscriptionState != null) {
                if (uiState.subscriptionState?.hasPremiumPlan == false) {
                    Text(
                        text = stringResource(MR.strings.plans_management_info_not_subscribed),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(40.dp))
                    PremiumPlanBanner(onClick = openBillingManagementScreen)
                } else if (uiState.subscriptionState?.isCurrentPlatform == true) {
                    Text(
                        text = stringResource(
                            MR.strings.plans_management_info_plan,
                            uiState.subscriptionState?.name ?: "---"
                        ).annotatedBold(),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(40.dp))
                    SettingsActionItem(
                        name = stringResource(MR.strings.plans_management_action_manage),
                        icon = CoreAppIcons.Edit.asPainterResource(),
                        type = SettingsActionItemType.Link
                    ) {
                        openBillingStore()
                    }

                    SettingsActionItem(
                        name = stringResource(MR.strings.plans_management_action_change),
                        icon = CoreAppIcons.Edit.asPainterResource(),
                        type = SettingsActionItemType.Navigation
                    ) {
                        openBillingManagementScreen()
                    }
                } else {
                    Text(
                        text = stringResource(MR.strings.plans_management_info_wrong_platform),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = stringResource(MR.strings.plans_management_info_not_subscribed),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(40.dp))
                PremiumPlanBanner(onClick = openBillingManagementScreen)
            }
        }
    }
}
