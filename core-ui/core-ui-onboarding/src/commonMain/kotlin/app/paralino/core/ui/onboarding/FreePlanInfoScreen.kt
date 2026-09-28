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
import app.paralino.core.presentation.onboarding.AccountOnboardingContent
import app.paralino.core.ui.onboarding.account.AccountOnboardingScreenContent
import app.paralino.core.presentation.onboarding.AccountOnboardingUiEvent

@Composable
fun FreePlanInfoScreenCore(
    coreUiState: CoreUiState,
    vm: PlanInfoViewModel,
    paddingValues: PaddingValues,
    onContinue: () -> Unit,
) {
    MessageDispatcherEffect(vm, coreUiState)
    val uiState by vm.uiState.collectAsState()
    AccountOnboardingScreenContent(
        modifier = Modifier.padding(paddingValues),
        coreUiState = coreUiState,
        step = AccountOnboardingContent.GroupOnboardingIntro,
        onEvent = {
            if (it == AccountOnboardingUiEvent.ConfirmOnboarding) {
                vm.confirmFreePlan()
                onContinue()
            }
        }
    )
}
