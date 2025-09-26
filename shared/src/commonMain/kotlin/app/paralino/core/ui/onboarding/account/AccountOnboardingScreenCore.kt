package app.paralino.core.ui.onboarding.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.presentation.onboarding.AccountOnboardingScreenEvent
import app.paralino.core.presentation.onboarding.AccountOnboardingScreenUiEvent
import app.paralino.core.presentation.onboarding.AccountOnboardingViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest


@Composable
fun AccountOnboardingScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: AccountOnboardingViewModel,
    onCreateGroup: () -> Unit,
    openPersonalCodes: () -> Unit,
    openPaywall: (BillingScreenSourceType) -> Unit,
    onSharePersonalCode: (String) -> Unit,
    askNotificationPermission: () -> Unit
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                AccountOnboardingScreenEvent.AskNotificationPermission -> askNotificationPermission()
                is AccountOnboardingScreenEvent.SharePersonalCode -> onSharePersonalCode(event.code)
                AccountOnboardingScreenEvent.OpenPersonalCodes -> openPersonalCodes()
                AccountOnboardingScreenEvent.OpenGroupCreate -> onCreateGroup()
                is AccountOnboardingScreenEvent.OpenPaywall -> openPaywall(event.type)

                null -> {
                    /* do nothing */
                }

            }
        }
    }

    when (val step = state.data?.onboardingStep) {
        is AccountOnboardingContent -> {
            AccountOnboardingScreenContent(
                modifier = modifier,
                coreUiState = appState,
                step = step,
                onEvent = {
                    vm.onEvent(AccountOnboardingScreenUiEvent.OnboardingStep(event = it))
                }
            )
        }
        else -> {}
    }

    if (state.data?.fullScreenLoading == true) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}