package app.paralino.core.ui.groups.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.detail.GroupSpaceFocusedFeatureChildItem
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.LargeSwitchSize
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun GroupSpaceDetailsHeader(
    modifier: Modifier = Modifier,
    isPersonalSpace: Boolean,
    groupName: String,
    groupImage: ImageModel?,
    sharingStatus: LocationSharingState?,
    featurePlaces: List<GroupSpaceFocusedFeatureChildItem>,
    featurePrivacyZones: List<GroupSpaceFocusedFeatureChildItem>,
    onMapClick: () -> Unit,
    onPlacesClick: () -> Unit,
    onPrivacyZonesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSharingClick: () -> Unit,
    onPlaceClick: (GroupSpaceFocusedFeatureChildItem) -> Unit,
    onPrivacyZoneClick: (GroupSpaceFocusedFeatureChildItem) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (groupImage != null) {
                ImageModelImage(
                    imageModel = groupImage,
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            0.5.dp,
                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                            RoundedCornerShape(10.dp)
                        )
                )
                Spacer(modifier = Modifier.size(16.dp))
            }

            Text(
                text = groupName,
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.size(30.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LargeSeekbarSwitch(
                modifier = Modifier
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
                size = LargeSwitchSize(minHeight = 90.dp),
                onStateChange = {
                    onSettingsClick()
                }
            )
            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
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
                size = LargeSwitchSize(minHeight = 90.dp),
                onStateChange = {
                    onMapClick()
                }
            )
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GroupSpaceFocusedFeatureElement(
                modifier = Modifier.clickable(onClick = onSharingClick),
                title = if (isPersonalSpace) {
                    stringResource(MR.strings.personalSpace_tracking_title)
                } else {
                    stringResource(MR.strings.group_details_sharing_action)
                },
                icon = if (sharingStatus != null) {
                    CoreAppIcons.ShareLocation.asPainterResource()
                } else {
                    CoreAppIcons.LocationOff.asPainterResource()
                },
                items = null,
                stateType = if (sharingStatus != null) {
                    GroupSpaceFocusedFeatureElementStateType.ACTIVE
                } else {
                    GroupSpaceFocusedFeatureElementStateType.INACTIVE
                },
                bottomExtraContent = {
                    if (sharingStatus != null) {
                        GroupDetailSharingStatusElement(
                            modifier = Modifier.padding(bottom = 16.dp),
                            contract = sharingStatus,
                            isPersonalSpace = isPersonalSpace
                        )
                    }
                },
                onChildItemClick = {}
            )

            GroupSpaceFocusedFeatureElement(
                modifier = Modifier.clickable(onClick = onPlacesClick),
                title = stringResource(MR.strings.places_list_screen_title),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                items = featurePlaces,
                stateType = if (featurePlaces.isNotEmpty()) {
                    GroupSpaceFocusedFeatureElementStateType.ACTIVE
                } else {
                    GroupSpaceFocusedFeatureElementStateType.IDLE
                },
                bottomExtraContent = null,
                onChildItemClick = onPlaceClick
            )

            if (!isPersonalSpace) {
                GroupSpaceFocusedFeatureElement(
                    modifier = Modifier.clickable(onClick = onPrivacyZonesClick),
                    title = stringResource(MR.strings.privacy_zones_list_screen_title),
                    icon = CoreAppIcons.PrivacyZone.asPainterResource(),
                    items = featurePrivacyZones,
                    stateType = if (featurePrivacyZones.isNotEmpty()) {
                        GroupSpaceFocusedFeatureElementStateType.ACTIVE
                    } else {
                        GroupSpaceFocusedFeatureElementStateType.IDLE
                    },
                    bottomExtraContent = null,
                    onChildItemClick = onPrivacyZoneClick
                )
            }
        }

        Spacer(modifier = Modifier.size(20.dp))
    }
}