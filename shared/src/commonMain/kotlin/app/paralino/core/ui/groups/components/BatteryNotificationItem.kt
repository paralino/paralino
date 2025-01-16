package app.paralino.core.ui.groups.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.groups.settings.BatteryNotificationMemberState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.theme.ButtonStyle

@Composable
fun BatteryNotificationItem(
    modifier: Modifier = Modifier,
    state: BatteryNotificationMemberState,
    onNotifyChange: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { if (!state.loading) onNotifyChange(state.memberUserId) }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 6.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .size(42.dp)
                    .border(
                        width = if (state.isCurrentUser) 2.dp else 0.5.dp,
                        color = if (state.isCurrentUser) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = state.name,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
            )
        }

        BatteryNotificationToggle(
            modifier = Modifier.size(42.dp, 42.dp),
            state = state.notify,
            loading = state.loading,
            onChange = { onNotifyChange(state.memberUserId) }
        )
    }
}

@Composable
fun BatteryNotificationToggle(
    modifier: Modifier = Modifier,
    state: Boolean,
    loading: Boolean,
    onChange: (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (state) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.surface
        }
    )
    val contentColor by animateColorAsState(
        targetValue = if (state) {
            MaterialTheme.colors.onPrimary
        } else {
            MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
        }
    )
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor
    )
    FilledButton(
        modifier = modifier,
        style = ButtonStyle.SMALL_ACTION(
            paddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            minContentHeight = 16.dp,
            shape = RoundedCornerShape(8.dp),
            iconSize = 22.dp,
            iconSpacing = 0.dp
        ),
        icon = if (state)
            CoreAppIcons.NotificationsActive.asPainterResource()
        else
            CoreAppIcons.NotificationsOff.asPainterResource(),
        elevation = null,
        state = AppButtonState.loadingState(loading),
        colors = colors,
        onClick = {
            if (!loading)
                onChange(!state)
        },
        content = {}
    )
}
