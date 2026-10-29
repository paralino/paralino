package app.paralino.android.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.history.LocationHistoryScreenEvent
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.utils.extensions.openNavigation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LocationHistoryScreenEventHandler(
    appState: MainAppState,
    uiState: LocationHistoryScreenUiState,
    events: Flow<LocationHistoryScreenEvent?>?,
    onDialogEvent: (LocationHistoryUiEvent.DialogAction) -> Unit,
    onCloseHistory: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        events?.collectLatest { event ->
            when (event) {
                LocationHistoryScreenEvent.NavigateBack -> {
                    onCloseHistory()
                }

                is LocationHistoryScreenEvent.OpenNavigation -> {
                    context.openNavigation(event.event)
                }

                LocationHistoryScreenEvent.OpenHistorySettings -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.LocationHistoryDelete(groupId = uiState.groupOrSpaceId.id)
                    )
                }

                LocationHistoryScreenEvent.OpenPaywall -> {
                    appState.navController.navigate(
                        AppScreen.Common.BillingPaywall(
                            type = BillingScreenSourceType.UPGRADE_DEFAULT
                        )
                    )
                }

                is LocationHistoryScreenEvent.OpenRawDataScreen -> {
                    appState.navController.navigate(
                        AppScreen.Common.RawDataScreen(
                            type = event.type
                        )
                    )
                }

                is LocationHistoryScreenEvent.OpenCreatePlace -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PlaceCreate(
                            groupId = event.groupId,
                            placeId = null,
                            editStep = null,
                            lat = event.location?.lat,
                            lng = event.location?.lng
                        )
                    )
                }

                is LocationHistoryScreenEvent.OpenCreatePrivacyZone -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.PrivacyZoneCreate(
                            groupId = event.groupId,
                            zoneId = null,
                            lat = event.location?.lat,
                            lng = event.location?.lng
                        )
                    )
                }

                null -> {}
            }
        }
    }

    val dialog = uiState.alertDialog
    dialog?.Show {
        onDialogEvent(LocationHistoryUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }
}