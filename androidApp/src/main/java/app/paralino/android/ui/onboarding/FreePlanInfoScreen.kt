package app.paralino.android.ui.onboarding

import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.ui.onboarding.FreePlanInfoScreenCore
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import org.koin.androidx.compose.koinViewModel

@Composable
fun FreePlanInfoScreen(
    appState: MainAppState,
    vm: PlanInfoViewModel = koinViewModel()
) {
    FreePlanInfoScreenCore(
        coreUiState = appState,
        showBanner = true,
        vm = vm,
        onContinue = {
            appState.navController.navigate(
                AppScreen.MainHome.GroupCreate.route(groupId = null)
            ) {
                this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                    this.inclusive = false
                }
            }
        },
        openPaywall = {
            appState.navController.navigate(
                AppScreen.MainHome.BillingPaywall.route(
                    groupId = null,
                    type = BillingScreenSourceType.FREE_PLAN_NOTICE
                )
            )
        }
    )
}