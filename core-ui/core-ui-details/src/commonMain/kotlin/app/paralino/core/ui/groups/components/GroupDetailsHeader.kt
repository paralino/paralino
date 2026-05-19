package app.paralino.core.ui.groups.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun GroupDetailsHeader(
    modifier: Modifier = Modifier.Companion,
    groupName: String,
    groupImage: ImageModel,
    onMapClick: () -> Unit,
    onPlacesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSharingClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            ImageModelImage(
                imageModel = groupImage,
                modifier = Modifier.Companion
                    .size(56.dp)
                    .border(
                        0.5.dp,
                        MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                        RoundedCornerShape(10.dp)
                    )
            )
            Spacer(modifier = Modifier.Companion.size(16.dp))
            Text(
                text = groupName,
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Companion.Bold),
                modifier = Modifier.Companion.weight(1f)
            )
        }

        Spacer(modifier = Modifier.Companion.size(30.dp))

        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LargeSeekbarSwitch(
                modifier = Modifier.Companion
                    .padding(end = 10.dp)
                    .fillMaxWidth(0.5f),
                icon = CoreAppIcons.Map.asPainterResource(),
                title = stringResource(MR.strings.title_map),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onMapClick()
                }
            )
            LargeSeekbarSwitch(
                modifier = Modifier.Companion.fillMaxWidth(),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                title = stringResource(MR.strings.places_list_screen_title),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onPlacesClick()
                }
            )
        }

        Spacer(modifier = Modifier.Companion.size(10.dp))

        Row(
            modifier = Modifier.Companion.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LargeSeekbarSwitch(
                modifier = Modifier.Companion
                    .padding(end = 10.dp)
                    .fillMaxWidth(0.5f),
                icon = CoreAppIcons.Settings.asPainterResource(),
                title = stringResource(MR.strings.title_settings),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onSettingsClick()
                }
            )
            LargeSeekbarSwitch(
                modifier = Modifier.Companion.fillMaxWidth(),
                icon = CoreAppIcons.ShareLocation.asPainterResource(),
                title = stringResource(MR.strings.group_details_sharing_action),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onSharingClick()
                }
            )
        }

        Spacer(modifier = Modifier.Companion.size(20.dp))
    }
}