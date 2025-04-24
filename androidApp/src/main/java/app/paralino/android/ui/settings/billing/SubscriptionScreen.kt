package app.paralino.android.ui.settings.billing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.presentation.settings.billing.SubscriptionViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.settings.billing.SubscriptionScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionScreen(
    appState: MainAppState,
    vm: SubscriptionViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding().fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.plans_management_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        val uriHandler = LocalUriHandler.current
        SubscriptionScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it,
            onNavigateBack = {
                appState.navController.popBackStack()
            },
            openBillingStore = {
                uriHandler.openUri(BackendResourceIDs.subscriptionUrlGooglePlay)
            },
            openPaywall = {
                appState.navController.navigate(AppScreen.Settings.BillingPaywall.route(type = BillingScreenSourceType.NORMAL))
            }
        )
    }
}
