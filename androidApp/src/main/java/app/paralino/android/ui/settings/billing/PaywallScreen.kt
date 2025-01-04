package app.paralino.android.ui.settings.billing

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.provider.BillingProviderAndroid
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.getActivity
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.PurchaseErrorException
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.settings.billing.PaywallScreenCore
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PaywallScreen(
    appState: MainAppState,
    groupId: String? = null,
    type: BillingScreenSourceType,
    vm: PaywallViewModel = koinViewModel { parametersOf(type) }
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

    val activity = LocalContext.current.getActivity()
    val scope = rememberCoroutineScope()

    PaywallScreenCore(
        appState = appState,
        vm = vm,
        onNavigateBack = { isSuccess -> goBack(isSuccess) },
        onStartPurchase = {
            val billingProvider = vm.billingProvider as BillingProviderAndroid
            scope.launch {
                try {
                    billingProvider.startPurchase(
                        activity = activity,
                        planType = it.planType,
                        newProductPlanId = it.newPlanProductId
                    )
                    vm.onEvent(PaywallScreenUiEvent.RefreshPlans)
                } catch (e: PurchaseErrorException) {
                    vm.onEvent(PaywallScreenUiEvent.OnPurchaseError(e))
                } catch (e: Exception) {
                    vm.onEvent(
                        PaywallScreenUiEvent.OnPurchaseError(
                            PurchaseErrorException(
                                errorCode = 0,
                                errorMessage = "Unknown Error",
                                userCancelled = false
                            )
                        )
                    )
                }
            }
        },
        onRestorePurchase = {
            val billingProvider = vm.billingProvider as BillingProviderAndroid
            scope.launch {
                try {
                    billingProvider.restorePurchase()
                    vm.onEvent(PaywallScreenUiEvent.RefreshPlans)
                } catch (e: PurchaseErrorException) {
                    vm.onEvent(PaywallScreenUiEvent.OnPurchaseError(e))
                } catch (e: Exception) {
                    vm.onEvent(
                        PaywallScreenUiEvent.OnPurchaseError(
                            PurchaseErrorException(
                                errorCode = 0,
                                errorMessage = "Unknown Error",
                                userCancelled = false
                            )
                        )
                    )
                }
            }
        }
    )

    BackHandler (enabled = type == BillingScreenSourceType.ONBOARDING) {
        // block back for onboarding
    }
}