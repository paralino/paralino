package app.paralino.core.ui.places

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.places.detail.PlaceSettingsState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PlaceDetailUserListItem(
    modifier: Modifier = Modifier,
    state: PlaceSettingsState,
    onEnterChange: (String) -> Unit,
    onExitChange: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .size(42.dp)
                    .border(
                        width = if (state.isCurrentUserOrDevice) 2.dp else 0.5.dp,
                        color = if (state.isCurrentUserOrDevice) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Spacer(modifier = Modifier.size(12.dp))
            Column(
                modifier = Modifier
                    .offset(y = if (state.tag.isNullOrBlank()) 0.dp else (-2).dp)
            ) {
                Text(
                    modifier = Modifier,
                    text = state.name,
                    style = MaterialTheme.typography.subtitle1,
                )
                if (!state.tag.isNullOrBlank()) {
                    Text(
                        text = state.tag,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colors.onSecondary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.secondary,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            PlaceSettingNotifyButton(
                modifier = Modifier.weight(1f),
                state = state.notifyEnter,
                text = stringResource(MR.strings.place_detail_setting_button_notify_enter),
                loading = state.loadingEnter,
            ) {
                onEnterChange(state.alertItemId)
            }

            Spacer(modifier = Modifier.size(4.dp))

            PlaceSettingNotifyButton(
                modifier = Modifier.weight(1f),
                state = state.notifyExit,
                text = stringResource(MR.strings.place_detail_setting_button_notify_exit),
                loading = state.loadingExit,
            ) {
                onExitChange(state.alertItemId)
            }
        }
    }
}

@Composable
fun PlaceSettingNotifyButton(
    modifier: Modifier = Modifier,
    state: Boolean,
    text: String,
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
            paddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            minContentHeight = 16.dp,
            shape = RoundedCornerShape(8.dp),
            iconSize = 18.dp,
            iconSpacing = 8.dp
        ),
        icon = if (state)
            CoreAppIcons.NotificationsActive.asPainterResource()
        else
            CoreAppIcons.NotificationsOff.asPainterResource(),
        elevation = 0.dp,
        state = AppButtonState.loadingState(loading),
        colors = colors,
        onClick = {
            if (!loading)
                onChange(!state)
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption
        )
    }
}