package app.paralino.android.ui.places.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.ui.components.AppButtonState
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.places.detail.PlaceSettingsState
import app.paralino.core.utils.Emojis
import app.paralino.android.R

@Composable
fun PlaceDetailUserListItem(
    modifier: Modifier = Modifier,
    state: PlaceSettingsState,
    onEnterChange: (Boolean) -> Unit,
    onExitChange: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageModelImage(
                imageModel = state.image,
                modifier = Modifier
                    .size(46.dp)
                    .border(
                        0.5.dp,
                        MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                        RoundedCornerShape(10.dp)
                    )
            )

            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = state.name,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            PlaceSettingNotifyButton(
                state = state.notifyEnter,
                text = stringResource(id = R.string.place_detail_setting_button_notify_enter),
                loading = state.loadingEnter,
            ) {
                onEnterChange(it)
            }

            Spacer(modifier = Modifier.size(8.dp))

            PlaceSettingNotifyButton(
                state = state.notifyExit,
                text = stringResource(id = R.string.place_detail_setting_button_notify_exit),
                loading = state.loadingExit,
            ) {
                onExitChange(it)
            }
        }
    }
}

@Composable
fun PlaceSettingNotifyButton(
    state: Boolean,
    text: String,
    loading: Boolean,
    onChange: (Boolean) -> Unit
) {
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = if(state) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
    )
    FilledButton(
        style = ButtonStyle.SMALL_ACTION(
            paddingValues = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            minContentHeight = 16.dp,
            shape = RoundedCornerShape(8.dp),
            iconSize = 18.dp,
            iconSpacing = 8.dp
        ),
        icon = if (state)
            Icons.Rounded.NotificationsActive
        else
            Icons.Rounded.NotificationsOff,
        elevation = ButtonDefaults.elevation(defaultElevation = if(state) 2.dp else 0.dp),
        state = AppButtonState.loadingState(loading),
        colors = colors,
        onClick = {
            if (loading)
                onChange(!state)
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2.copy(fontSize = 12.sp)
        )
    }
}

@Preview("place_item")
@Composable
fun PlaceListItemPreview() {
    AppTheme {
        Column {
            val item = PlaceSettingsState(
                groupId = "",
                placeId = "",
                memberId = "",
                name = "Some user",
                image = ImageModel.emoji(Emojis.group1.first()),
                notifyEnter = false,
                notifyExit = false,
                loadingEnter = false,
                loadingExit = false
            )
            PlaceDetailUserListItem(
                state = item,
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(notifyEnter = true, notifyExit = true),
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(notifyEnter = true, loadingEnter = true),
                onEnterChange = {},
                onExitChange = {}
            )
            PlaceDetailUserListItem(
                state = item.copy(loadingExit = true),
                onEnterChange = {},
                onExitChange = {}
            )
        }
    }
}