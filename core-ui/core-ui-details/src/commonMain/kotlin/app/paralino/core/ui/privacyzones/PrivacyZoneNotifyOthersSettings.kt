package app.paralino.core.ui.privacyzones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.rememberSeekbarState
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun PrivacyZoneNotifyOthersSettings(
    modifier: Modifier = Modifier,
    focusManager: FocusManager,
    notifyOthersInZone: Boolean,
    inZoneMessage: String?,
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
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(70.dp),
                text = inZoneMessage.orEmpty(),
                hint = stringResource(MR.strings.privacy_zones_create_status_message_placeholder),
                hasError = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                focusManager = focusManager,
                onInputChange = onInZoneMessageChanged,
            )
        }
    }
}