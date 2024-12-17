package app.paralino.android.ui.onboarding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.ui.onboarding.FreePlanInfoScreenCore
import org.koin.androidx.compose.koinViewModel

@Composable
fun FreePlanInfoScreen(
    appState: MainAppState,
    vm: PlanInfoViewModel = koinViewModel()
) {
    FreePlanInfoScreenCore(
        coreUiState = appState,
        vm = vm,
        paddingValues = remember { PaddingValues() },
        onContinue = {
            appState.navController.navigate(
                AppScreen.MainHome.GroupCreate.route(groupId = null)
            ) {
                this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                    this.inclusive = false
                }
            }
        }
    )
}