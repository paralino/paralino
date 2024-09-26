package app.paralino.android.ui.settings.billing

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.settings.subscriptions.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.subscriptions.PaywallViewModel
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.settings.billing.PaywallScreenCore
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PaywallScreen(
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
                title = if (type == BillingScreenSourceType.ONBOARDING) {
                    null
                } else {
                    stringResource(resource = MR.strings.plans_banner_settings_title)
                },
                navigationIcon = if (type == BillingScreenSourceType.ONBOARDING) {
                    null
                } else {
                    { ToolbarButtons.Close { goBack(isSuccess = false) } }
                },
                actions = {
                    if (type == BillingScreenSourceType.ONBOARDING) {
                        SmallActionButton(
                            modifier = Modifier.padding(end = 8.dp),
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary,
                            icon = null,
                            hasEndArrow = true,
                            text = {
                                Text(
                                    text = stringResource(MR.strings.plans_paywall_onboarding_skip_button),
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                goBack(isSuccess = false)
                            }
                        )
                    }
                }
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