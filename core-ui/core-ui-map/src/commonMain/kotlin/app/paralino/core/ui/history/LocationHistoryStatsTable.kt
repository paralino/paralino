package app.paralino.core.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.history.LocationHistoryStats
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import org.jetbrains.compose.resources.DrawableResource

private data class StatInfo(
    val label: String,
    val value: String,
    val icon: DrawableResource
)

@Composable
fun LocationHistoryStatsTable(
    modifier: Modifier = Modifier,
    isCompactMode: Boolean,
    stats: LocationHistoryStats?
) {
    val statsList = remember(stats) {
        listOf(
            StatInfo(
                label = MR.strings.history_stats_distance.localizedString(),
                value = stats?.formattedDistance ?: "-",
                icon = CoreAppIcons.PolylineWithDots
            ),
            StatInfo(
                label = MR.strings.history_stats_entries.localizedString(),
                value = stats?.locationEntriesCount?.toString() ?: "-",
                icon = CoreAppIcons.LocationOn
            ),
            StatInfo(
                label = MR.strings.history_stats_max_speed.localizedString(),
                value = stats?.formattedMaxSpeed ?: "-",
                icon = CoreAppIcons.Speed
            ),
            StatInfo(
                label = MR.strings.history_stats_networks.localizedString(),
                value = stats?.networks?.joinToString(", ")?.ifEmpty { "-" } ?: "-",
                icon = CoreAppIcons.SignalCell3
            ),
            StatInfo(
                label = MR.strings.history_stats_avg_battery.localizedString(),
                value = stats?.averageBattery?.let { "$it%" } ?: "-",
                icon = CoreAppIcons.BatteryFull),
            StatInfo(
                label = MR.strings.history_stats_battery_range.localizedString(),
                value = stats?.batteryRange?.let {
                    "${stats.batteryRange.first}% - ${stats.batteryRange.second}%"
                } ?: "-",
                icon = CoreAppIcons.BatteryChargingFull
            ),
            StatInfo(
                label = MR.strings.history_stats_elevation_gain.localizedString(),
                value = stats?.formattedElevationGain ?: "-",
                icon = CoreAppIcons.Elevation
            ),
            StatInfo(
                label = MR.strings.history_stats_elevation_range.localizedString(),
                value = stats?.formattedElevationRange ?: "-",
                icon = CoreAppIcons.Elevation
            )
        )
    }

    BoxWithConstraints(modifier = modifier) {
        if (isCompactMode) {
            FlowColumn(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colors.surface)
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachColumn = 2
            ) {
                statsList.forEach { stat ->
                    StatItem(
                        modifier = Modifier.weight(1f).widthIn(min = this@BoxWithConstraints.maxWidth * 0.25f),
                        label = stat.label,
                        value = stat.value
                    )
                }
            }
        } else {
            FlowRow(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colors.surface),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                maxItemsInEachRow = 2
            ) {
                statsList.forEach { stat ->
                    StatItem(
                        modifier = Modifier.weight(1f),
                        label = stat.label,
                        value = stat.value
                    )
                }
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
