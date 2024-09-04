package app.paralino.core.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.settings.subscriptions.SubscriptionScreenEvent
import app.paralino.core.presentation.settings.subscriptions.SubscriptionScreenState
import app.paralino.core.presentation.settings.subscriptions.SubscriptionScreenUiEvent
import app.paralino.core.presentation.settings.subscriptions.SubscriptionViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
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
    val uiState by vm.uiState.collectAsState(SubscriptionScreenState())

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
}
