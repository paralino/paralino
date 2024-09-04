package app.paralino.android.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.presentation.settings.subscriptions.SubscriptionViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.SubscriptionScreenCore
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import org.koin.androidx.compose.getViewModel

@Composable
fun SubscriptionScreen(
    appState: MainAppState,
    vm: SubscriptionViewModel = getViewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = "",
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        val uriHandler = LocalUriHandler.current
        SubscriptionScreenCore(
            appState = appState,
            vm = vm,
            paddingValues = it,
            openBillingStore = {
                uriHandler.openUri(BackendResourceIDs.subscriptionUrlGooglePlay)
            },
            openBillingManagementScreen = {
                appState.navController.navigate(AppScreen.Settings.BillingPaywall.routeSignature)
            }
        )
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun BillingScreen(
    appState: MainAppState
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = "",
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        val paywallOptions = remember {
            PaywallOptions.Builder(
                dismissRequest = {
                    appState.navController.popBackStack()
                }
            )
                .setShouldDisplayDismissButton(false)
                .build()
        }
        Paywall(options = paywallOptions)
    }
}