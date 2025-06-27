package app.paralino.core.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
fun LocationHistoryStatsTable(
    modifier: Modifier = Modifier,
    stats: LocationHistoryStats?
) {
    val statsList = remember(stats) {
        listOf(
            StatInfo("Distance", stats?.formattedDistance ?: "-", CoreAppIcons.RouteAltBold),
            StatInfo(
                "Entries",
                stats?.locationEntriesCount?.toString() ?: "-",
                CoreAppIcons.LocationOn
            ),
            StatInfo("Max Speed", stats?.formattedMaxSpeed ?: "-", CoreAppIcons.Speed),
            StatInfo(
                "Networks",
                stats?.networks?.joinToString(", ")?.ifEmpty { "-" } ?: "-",
                CoreAppIcons.SignalCell3
            ),
            StatInfo(
                "Avg. Battery",
                stats?.averageBattery?.let { "$it%" } ?: "-",
                CoreAppIcons.BatteryFull),
            StatInfo(
                "Battery Range", stats?.batteryRange?.let {
                    "${stats.batteryRange.first}% - ${stats.batteryRange.second}%"
                } ?: "-",
                CoreAppIcons.BatteryChargingFull
            ),
            StatInfo(
                "Elevation Gain",
                stats?.formattedElevationGain ?: "-",
                CoreAppIcons.Elevation
            ),
            StatInfo(
                "Elevation Range",
                stats?.formattedElevationRange ?: "-",
                CoreAppIcons.Elevation
            ),
        )
    }

    Column(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
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
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    }
}
