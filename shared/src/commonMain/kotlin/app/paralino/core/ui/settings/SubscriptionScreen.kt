package app.paralino.core.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.settings.subscriptions.SubscriptionScreenState
import app.paralino.core.presentation.settings.subscriptions.SubscriptionScreenUiEvent
import app.paralino.core.presentation.settings.subscriptions.SubscriptionViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show

@Composable
fun SubscriptionScreenCore(
    appState: CoreUiState,
    vm: SubscriptionViewModel,
    paddingValues: PaddingValues,
    openBillingStore: () -> Unit,
    openBillingManagementScreen: () -> Unit,
) {
    val uiState by vm.uiState.collectAsState(SubscriptionScreenState())

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

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        if (uiState.subscriptionState != null) {
            Text(text = "Plan: ${uiState.subscriptionState?.name}")
            Text(text = "ExpiresAt: ${uiState.subscriptionState?.expiresAt}")
            Text(text = "Period: ${uiState.subscriptionState?.billingPeriod}")
            Text(text = "isFromParalino: ${uiState.subscriptionState?.isFromParalino}")

            if (uiState.subscriptionState?.hasPremiumPlan == false) {
                SettingsActionItem(
                    name = "Select plan",
                    icon = CoreAppIcons.Edit.asPainterResource(),
                    type = SettingsActionItemType.Navigation
                ) {
                    openBillingManagementScreen()
                }
            } else if (uiState.subscriptionState?.isCurrentPlatform == true) {
                SettingsActionItem(
                    name = "Cancel subscription",
                    icon = CoreAppIcons.Block.asPainterResource(),
                    type = SettingsActionItemType.Navigation
                ) {
                    openBillingStore()
                }

                SettingsActionItem(
                    name = "Change plan",
                    icon = CoreAppIcons.Edit.asPainterResource(),
                    type = SettingsActionItemType.Navigation
                ) {
                    openBillingManagementScreen()
                }
            } else {
                Text(text = "You can not manage this plan on this device.\nUse device you originally used for purchase.")
            }
        } else {
            Text(text = "No plan")
            SettingsActionItem(
                name = "Select plan",
                icon = CoreAppIcons.Edit.asPainterResource(),
                type = SettingsActionItemType.Navigation
            ) {
                openBillingManagementScreen()
            }
        }

        Spacer(Modifier.size(20.dp))
    }
}
