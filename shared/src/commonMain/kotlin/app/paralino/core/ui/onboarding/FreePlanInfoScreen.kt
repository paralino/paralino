package app.paralino.core.ui.onboarding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.onboarding.account.AccountOnboardingContent
import app.paralino.core.ui.onboarding.account.AccountOnboardingScreen
import app.paralino.core.ui.onboarding.account.AccountOnboardingUiEvent

@Composable
fun FreePlanInfoScreenCore(
    coreUiState: CoreUiState,
    vm: PlanInfoViewModel,
    paddingValues: PaddingValues,
    onContinue: () -> Unit,
) {
    MessageDispatcherEffect(vm, coreUiState)
    val uiState by vm.uiState.collectAsState()
    AccountOnboardingScreen(
        modifier = Modifier.padding(paddingValues),
        coreUiState = coreUiState,
        step = AccountOnboardingContent.SilverPlan(planConfig = uiState),
        onEvent = {
            if (it == AccountOnboardingUiEvent.ConfirmOnboarding) {
                vm.confirmFreePlan()
                onContinue()
            }
        }
    )
}
