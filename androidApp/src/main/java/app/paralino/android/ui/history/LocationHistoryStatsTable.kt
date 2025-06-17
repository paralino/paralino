package app.paralino.android.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.history.LocationHistoryStats
import app.paralino.core.ui.components.CoreAppIcons
import org.jetbrains.compose.resources.DrawableResource

private data class StatInfo(
    val label: String,
    val value: String,
    val icon: DrawableResource
)

@Composable
fun LocationHistoryStatsTable(stats: LocationHistoryStats) {
    val statsList = remember(stats) {
        listOf(
            StatInfo("Distance", stats.formattedDistance, CoreAppIcons.RouteAlt),
            StatInfo("Entries", stats.locationEntriesCount.toString(), CoreAppIcons.LocationOn),
            StatInfo("Max Speed", stats.formattedMaxSpeed, CoreAppIcons.Speed),
            StatInfo("Networks", stats.networks.joinToString(", ").ifEmpty { "N/A" }, CoreAppIcons.SignalCell3),
            StatInfo("Avg. Battery", "${stats.averageBattery}%", CoreAppIcons.BatteryFull),
            StatInfo("Battery Range", "${stats.batteryRange.first}% - ${stats.batteryRange.second}%",
                CoreAppIcons.BatteryChargingFull),
            StatInfo("Elevation Gain", stats.formattedElevationGain, CoreAppIcons.Elevation),
            StatInfo("Elevation Range", stats.formattedElevationRange, CoreAppIcons.Elevation),
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Trip Statistics",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(statsList) { stat ->
                StatItem(label = stat.label, value = stat.value)
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LocationHistoryStatsTablePreview() {
    MaterialTheme {
        val sampleStats = LocationHistoryStats(
            batteryRange = 35 to 85,
            averageBattery = 65,
            locationEntriesCount = 253,
            networks = listOf("WIFI", "CELL"),
            formattedDistance = "12.3 km",
            formattedMaxSpeed = "45.1 km/h",
            formattedElevationRange = "120 - 200 m",
            formattedElevationGain = "340 m",
            formattedTime = "",
            totalDistanceMeters = 0f,
            maxSpeedMps = 2f,
            elevationGainMeters = 12f,
            elevationRangeMeters = 12f to 12f
        )
        Box(modifier = Modifier.padding(16.dp)) {
            LocationHistoryStatsTable(stats = sampleStats)
        }
    }
}