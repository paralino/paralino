package app.paralino.core.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.paralino.core.presentation.settings.subscriptions.PaywallScreenEvent
import app.paralino.core.presentation.settings.subscriptions.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.subscriptions.PaywallViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PaywallScreenCore(
    appState: CoreUiState,
    vm: PaywallViewModel,
    onNavigateBack: (Boolean) -> Unit,
    paywallContent: @Composable () -> Unit,
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PaywallScreenEvent.OnNavigateBack -> {
                    onNavigateBack(event.isSuccess)
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
                PaywallScreenUiEvent.DialogAction(
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
        paywallContent()
    }
}

enum class BillingScreenSourceType(val key: String) {
    ONBOARDING("onboarding"),
    FREE_PLAN_NOTICE("notice"),
    NORMAL("normal");

    companion object {
        fun fromKey(key: String?): BillingScreenSourceType {
            return BillingScreenSourceType.entries.firstOrNull { it.key == key } ?: NORMAL
        }
    }
}