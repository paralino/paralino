package app.paralino.core.ui.privacyzones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PrivacyZoneNotifyOthersSettings(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    notifyOthersInZone: Boolean,
    inZoneMessage: String?,
    maxMessageLength: Int,
    memberPreviewState: GroupMemberItemStateWithOneLocation,
    onNotifyOthersInZoneChanged: (Boolean) -> Unit,
    onInZoneMessageChanged: (String) -> Unit,
) {
    Column(modifier = modifier) {
        val notifyState by rememberSeekbarState(
            key = notifyOthersInZone,
            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                seekbarState = null,
                switchState = LargeSeekbarSwitchState.Switch(
                    checkState = notifyOthersInZone,
                )
            )
        )

        LargeSeekbarSwitch(
            modifier = Modifier.fillMaxWidth(),
            icon = CoreAppIcons.Edit.asPainterResource(),
            title = stringResource(MR.strings.privacy_zones_create_notify_others_title),
            description = if (notifyOthersInZone) {
                stringResource(MR.strings.privacy_zones_create_notify_others_description_on)
            } else {
                stringResource(MR.strings.privacy_zones_create_notify_others_description_off)
            },
            state = notifyState,
            onStateChange = onNotifyOthersInZoneChanged,
        )

        AnimatedVisibility(visible = notifyOthersInZone) {
            MainInputField(
                modifier = Modifier.padding(top = 8.dp),
                text = inZoneMessage.orEmpty(),
                hint = stringResource(MR.strings.privacy_zones_create_status_message_placeholder),
                hasError = false,
                maxTextLength = maxMessageLength,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                focusManager = focusManager,
                onInputChange = onInZoneMessageChanged,
            )
        }

        DataInfoChip(
            modifier = Modifier.padding(bottom = 4.dp, top = 20.dp).fillMaxWidth(),
            imageOrIcon = {
                ImageModelImage(
                    imageModel = ImageModel.emoji(Emojis.lightBulb),
                    modifier = Modifier.size(16.dp),
                    useBackgroundForEmoji = false,
                    clipShape = CircleShape
                )
                Spacer(Modifier.size(4.dp))
            },
            text = {
                Text(
                    text = stringResource(MR.strings.privacy_zones_create_member_preview),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
                )
            },
            endSection = null,
            backgroundColor = Color.Transparent,
            onClick = null
        )

        GroupMember(
            modifier = Modifier,
            state = memberPreviewState,
            forceFullExpandAction = true,
            collapseOnActionClick = false,
            canShowOldLocation = true,
            onEvent = {}
        )
    }
}