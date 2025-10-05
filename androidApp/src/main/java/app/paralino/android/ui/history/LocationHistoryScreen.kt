package app.paralino.android.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.history.LocationHistoryDateSelectorState
import app.paralino.core.presentation.history.LocationHistoryDayState
import app.paralino.core.presentation.history.LocationHistoryScreenEvent
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistorySelectedDayPosition
import app.paralino.core.presentation.history.LocationHistoryStats
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.history.LocationHistoryViewModel
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.history.LocationHistoryDateSelector
import app.paralino.core.ui.history.LocationHistoryScreenElements
import app.paralino.core.ui.history.LocationHistoryStatsTable
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.openNavigation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LocationHistoryScreen(
    appState: MainAppState,
    groupId: String,
    memberUserId: String,
    deviceId: String?,
    vm: LocationHistoryViewModel = koinViewModel { parametersOf(groupId, memberUserId, deviceId) },
) {
    MessageDispatcherEffect(vm, appState)

    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current

    LifecycleStartEffect(Unit) {
        vm.onEvent(LocationHistoryUiEvent.UiLifecycleChange(active = true))
        onStopOrDispose {
            vm.onEvent(LocationHistoryUiEvent.UiLifecycleChange(active = false))
        }
    }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                LocationHistoryScreenEvent.NavigateBack -> {
                    appState.navController.popBackStack()
                }

                is LocationHistoryScreenEvent.OpenNavigation -> {
                    context.openNavigation(event.event)
                }

                LocationHistoryScreenEvent.OpenHistorySettings -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.LocationHistoryDelete.route(groupId = uiState.groupOrSpaceId.id)
                    )
                }

                LocationHistoryScreenEvent.OpenPaywall -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.BillingPaywall.route(
                            type = BillingScreenSourceType.UPGRADE
                        )
                    )
                }

                null -> {}
            }
        }
    }

    val dialog = uiState.alertDialog
    dialog?.Show {
        vm.onEvent(LocationHistoryUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    MapContent(
        uiState = uiState,
        onEvent = { vm.onEvent(it) }
    )
}

@Composable
private fun MapContent(
    uiState: LocationHistoryScreenState,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    var cardHeight by remember { mutableStateOf(0.dp) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LocationHistoryMapContent(
            uiState = uiState,
            onEvent = onEvent,
            mapContentPadding = PaddingValues(
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 64.dp,
                bottom = (cardHeight + WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding()).coerceIn(
                    minimumValue = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                    maximumValue = this.maxHeight
                )
            )
        )

        LocationHistoryScreenElements(
            modifier = Modifier,
            uiState = uiState,
            topPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 4.dp,
            cardHeight = { cardHeight = it },
            onEvent = onEvent
        )
    }
}



@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationHistoryDateSelectorPreview() {
    val sampleDays = listOf(
        LocationHistoryDayState(
            isSelected = false,
            isLoading = false,
            date = LocalDate(2024, 1, 15),
            formattedDay = "Mon",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = true,
            isLoading = false,
            date = LocalDate(2024, 1, 16),
            formattedDay = "Tue",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.START,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = true,
            isLoading = false,
            date = LocalDate(2024, 1, 17),
            formattedDay = "Wed",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.CENTER,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = true,
            isLoading = false,
            date = LocalDate(2024, 1, 18),
            formattedDay = "Thu",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.END,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = false,
            isLoading = false,
            date = LocalDate(2024, 1, 19),
            formattedDay = "Fri",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = false,
            isLoading = false,
            date = LocalDate(2024, 1, 20),
            formattedDay = "Sat",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = true
        ),
        LocationHistoryDayState(
            isSelected = false,
            isLoading = false,
            date = LocalDate(2024, 1, 21),
            formattedDay = "Sun",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = true
        ),
        LocationHistoryDayState(
            isSelected = true,
            isLoading = false,
            date = LocalDate(2024, 1, 22),
            formattedDay = "Mon",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = false
        )
    )

    AppTheme {
        Surface {
            LocationHistoryDateSelector(
                modifier = Modifier.padding(16.dp),
                elevation = 8.dp,
                state = LocationHistoryDateSelectorState(
                    days = sampleDays.reversed(),
                    dateRangeDays = 7,
                    maxDateRangeDays = 7
                ),
                onEvent = { },
            )
        }
    }
}


@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LocationHistoryStatsTablePreview() {
    AppTheme {
        val sampleStats = LocationHistoryStats(
            batteryRange = 35 to 85,
            averageBattery = 65,
            locationEntriesCount = 253,
            networks = listOf("WIFI", "Cell"),
            formattedDistance = "12.3 km",
            formattedMaxSpeed = "45.1 km/h",
            formattedElevationRange = "120 - 200 m",
            formattedElevationGain = "340 m",
            formattedTimeRange = "",
            totalDistanceMeters = 0f,
            maxSpeedMps = 2f,
            elevationGainMeters = 12f,
            elevationRangeMeters = 12f to 12f
        )
        Box(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .padding(12.dp)
        ) {
            LocationHistoryStatsTable(stats = sampleStats)
        }
    }
}