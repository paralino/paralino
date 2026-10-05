package app.paralino.core.ui.settings.billing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.settings.billing.PaywallScreenEvent
import app.paralino.core.presentation.settings.billing.PaywallScreenStep
import app.paralino.core.presentation.settings.billing.PaywallScreenUiEvent
import app.paralino.core.presentation.settings.billing.PaywallViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.Show
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PaywallScreenCore(
    modifier: Modifier = Modifier,
    appState: CoreUiState,
    vm: PaywallViewModel,
    paddingValues: PaddingValues,
    onNavigateBack: (Boolean) -> Unit,
    onStartPurchase: (PaywallScreenEvent.OnStartPurchase) -> Unit,
    onRestorePurchase: (PaywallScreenEvent.OnRestorePurchase) -> Unit,
    openLicenceScreen: () -> Unit,
    openBillingStore: () -> Unit
) {
    MessageDispatcherEffect(vm, appState)
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is PaywallScreenEvent.OnNavigateBack -> onNavigateBack(event.isSuccess)
                is PaywallScreenEvent.OnStartPurchase -> onStartPurchase(event)
                is PaywallScreenEvent.OnRestorePurchase -> onRestorePurchase(event)
                is PaywallScreenEvent.OpenNativeBillingStorePage -> openBillingStore()

                null -> {
                    /* do nothing */
                }
            }
        }
    }

    uiState.alertDialog?.let { dialog ->
        dialog.Show {
            vm.onEvent(
                PaywallScreenUiEvent.DialogAction(
                    AlertDialogAction(
                        dialog = dialog,
                        action = it
                    )
                )
            )
        }
    }

    var isOfferDialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.screenStep) {
        if (uiState.screenStep == PaywallScreenStep.PAYWALL) {
            delay(1500)
            isOfferDialogVisible =
                !uiState.hasActivePlanOnAnotherPlatform && uiState.screenStep == PaywallScreenStep.PAYWALL
        } else {
            isOfferDialogVisible = false
        }
    }

    if (isOfferDialogVisible) {
        uiState.subscriptionOfferDialog?.let { dialog ->
            dialog.dialogOffer.Show {
                vm.onEvent(
                    PaywallScreenUiEvent.OfferDialogAction(
                        AlertDialogAction(
                            dialog = dialog,
                            action = it
                        )
                    )
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) {
        var bottomCardHeight by remember { mutableStateOf(0) }
        val focusManager = LocalFocusManager.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            OnboardingShowcaseStep(
                screenStep = uiState.screenStep,
                screenType = uiState.screenType,
                paddingValues = paddingValues
            )

            DemoToHistoryStep(
                memberName = uiState.memberName,
                memberImage = uiState.memberImageModel,
                deviceName = uiState.memberDeviceName,
                screenStep = uiState.screenStep,
                valueFormatter = vm.valueFormatter,
                paddingValues = paddingValues
            )

            PersonalHistoryStep(
                screenStep = uiState.screenStep,
                message = uiState.locationHistoryStepMessage
            )

            PurchaseConfirmationStep(
                screenStep = uiState.screenStep,
            )

            FullScreenLoadingStep(
                screenStep = uiState.screenStep,
                isLoading = uiState.isLoading
            )

            PaywallScreenBillingStep(
                uiState = uiState,
                paddingValues = paddingValues,
                bottomCardHeight = bottomCardHeight,
                valueFormatter = vm.valueFormatter,
                onNavigateBack = onNavigateBack,
                openLicenceScreen = openLicenceScreen,
                onEvent = vm::onEvent
            )

            PaywallBottomCard(
                uiState = uiState,
                paddingValues = paddingValues,
                onHeightChange = { bottomCardHeight = it },
                onEvent = vm::onEvent
            )
        }
    }
}
