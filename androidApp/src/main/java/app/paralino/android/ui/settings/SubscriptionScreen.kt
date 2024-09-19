package app.paralino.android.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.presentation.settings.subscriptions.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.subscriptions.PaywallViewModel
import app.paralino.core.presentation.settings.subscriptions.SubscriptionViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.BillingScreenSourceType
import app.paralino.core.ui.settings.PaywallScreenCore
import app.paralino.core.ui.settings.SubscriptionScreenCore
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import dev.icerock.moko.resources.compose.stringResource
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
            openBillingManagementScreen = {
                appState.navController.navigate(AppScreen.Settings.BillingPaywall.route())
            }
        )
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun BillingScreen(
    appState: MainAppState,
    groupId: String? = null,
    type: BillingScreenSourceType,
    vm: PaywallViewModel = getViewModel()
) {
    fun goBack(isSuccess: Boolean) {
        when (type) {
            BillingScreenSourceType.ONBOARDING -> {
                if (groupId.isNullOrBlank()) {
                    appState.navController.navigate(AppScreen.MainHome.MainMap.routeSignature)
                } else {
                    appState.navController.navigate(
                        AppScreen.MainHome.GroupDetails.route(groupId = groupId)
                    ) {
                        this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                            this.inclusive = false
                        }
                    }
                }
            }
            BillingScreenSourceType.FREE_PLAN_NOTICE -> {
                if (isSuccess) {
                    appState.navController.navigate(AppScreen.MainHome.GroupCreate.route(groupId = null)) {
                        this.popUpTo(route = AppScreen.MainHome.MainMap.routeSignature) {
                            this.inclusive = false
                        }
                    }
                } else {
                    appState.navController.popBackStack()
                }
            }
            BillingScreenSourceType.NORMAL -> {
                appState.navController.popBackStack()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = "",
                navigationIcon = { ToolbarButtons.Close { goBack(isSuccess = false) } }
            )
        },
    ) {
        PaywallScreenCore(
            appState = appState,
            vm = vm,
            onNavigateBack = { isSuccess -> goBack(isSuccess) }
        ) {
            var dismissBlock by remember {
                mutableStateOf(false)
            }
            val paywallOptions = remember {
                PaywallOptions.Builder(
                    dismissRequest = {
                        if (!dismissBlock) {
                            goBack(isSuccess = false)
                        }
                    }
                )
                    .setShouldDisplayDismissButton(false)
                    .setListener(
                        object : PaywallListener {
                            override fun onPurchaseCompleted(
                                customerInfo: CustomerInfo,
                                storeTransaction: StoreTransaction
                            ) {
                                dismissBlock = true
                                vm.onEvent(PaywallScreenUiEvent.RefreshPlans)
                            }

                            override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                                dismissBlock = true
                                vm.onEvent(PaywallScreenUiEvent.RefreshPlans)
                            }
                        }
                    )
                    .build()
            }
            Paywall(options = paywallOptions)
        }
    }
}