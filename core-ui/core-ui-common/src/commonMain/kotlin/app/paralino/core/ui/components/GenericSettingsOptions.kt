package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.settings.generic.GroupOptionItem

@Composable
fun GenericSettingsOptions(
    modifier: Modifier = Modifier,
    items: List<GroupOptionItem>,
    onOptionSelected: (GroupOptionItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colors.surface)
    ) {
        items.forEachIndexed { index, state ->
            if (index > 0) {
                Divider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = Color.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
            }

            GenericSettingsOptionItem(
                title = state.title,
                description = state.description,
                batteryImpact = state.tag,
                highBatteryImpact = state.isTagWarning,
                isSelected = state.isSelected,
                isLoading = state.isLoading,
                isPremiumFeatureEnabled = state.isPremiumFeatureEnabled,
                onClick = { onOptionSelected(state) }
            )
        }
    }
}