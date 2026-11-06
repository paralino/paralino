package app.paralino.android.ui.settings.billing

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import app.paralino.android.provider.BillingProviderAndroid
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.presentation.settings.billing.PurchaseErrorException
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.ui.settings.billing.PaywallScreenCore
import app.paralino.core.utils.extensions.getActivity
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
            BillingScreenSourceType.PERSONAL_SPACE_ONBOARDING -> {
                appState.navController.popBackStack(
                    popUpTo = AppScreen.MainHome.MainMap::class,
                    inclusive = false
                )
            }

            BillingScreenSourceType.GROUP_ONBOARDING,
            BillingScreenSourceType.FREE_PLAN_NEW_GROUP_MEMBER -> {
                if (groupId.isNullOrBlank()) {
                    appState.navController.navigate(AppScreen.MainHome.MainMap())
                } else {
                    appState.navController.navigate(
                        AppScreen.MainHome.GroupDetails(groupId = groupId),
                        popUpTo = AppScreen.MainHome.MainMap::class
                    )
                }
            }

            BillingScreenSourceType.NORMAL,
            BillingScreenSourceType.UPGRADE_MAP_SHEET,
            BillingScreenSourceType.UPGRADE_BILLING_PROMO,
            BillingScreenSourceType.SKIPPED_ONBOARDING,
            BillingScreenSourceType.UPGRADE_MORE_GROUPS,
            BillingScreenSourceType.UPGRADE_DEFAULT -> {
                appState.navController.popBackStack()
            }
        }
    }

    val activity = LocalContext.current.getActivity()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    PaywallScreenCore(
        modifier = Modifier,
        appState = appState,
        vm = vm,
        paddingValues = WindowInsets.safeDrawing.asPaddingValues(),
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
        },
        openLicenceScreen = {
            appState.navController.navigate(AppScreen.Settings.LicenceCode)
        },
        openBillingStore = {
            uriHandler.openUri(BackendResourceIDs.subscriptionUrlGooglePlay)
        }
    )

    BackHandler (enabled = type.isOnboarding) {
        // block back for onboarding
    }
}