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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            StatInfo("Distance", stats?.formattedDistance ?: "-", CoreAppIcons.Timeline),
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
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.surface),
            columns = GridCells.Fixed(2),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
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
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle1.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                color = MaterialTheme.colors.onSurface,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption.copy(
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    }
}
