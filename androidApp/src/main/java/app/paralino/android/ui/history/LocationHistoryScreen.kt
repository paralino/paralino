package app.paralino.android.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.presentation.history.LocationHistoryDateSelectorState
import app.paralino.core.presentation.history.LocationHistoryDayState
import app.paralino.core.presentation.history.LocationHistoryScreenEvent
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistorySelectedDayPosition
import app.paralino.core.presentation.history.LocationHistoryStats
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.history.LocationHistoryDateSelector
import app.paralino.core.ui.history.LocationHistoryStatsTable
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.extensions.openNavigation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate


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

                is LocationHistoryScreenEvent.OpenRawDataScreen -> {
                    appState.navController.navigate(
                        AppScreen.MainHome.RawDataScreen.route(
                            type = event.type
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
                backgroundColor = MaterialTheme.colors.background,
                elevation = 8.dp,
                cornerRadiusDp = 12.dp,
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