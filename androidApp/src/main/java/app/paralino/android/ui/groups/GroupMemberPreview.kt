package app.paralino.android.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberState
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberActionsState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.theme.AppTheme
import app.paralino.core.utils.Emojis

@Preview(device = Devices.PIXEL_3A)
@Composable
fun GroupMemberPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(modifier = Modifier.padding(40.dp)) {
                GroupMember(
                    state = GroupMemberItemStateWithOneLocation(
                        memberState = dummyMemberState,
                        location = null,
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        lastPing = null
                    )
                ) { }
                Spacer(modifier = Modifier.size(8.dp))
                GroupMember(
                    state = GroupMemberItemStateWithOneLocation(
                        memberState = dummyMemberState.copy(isVerified = false),
                        location = dummyLocations.first(),
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        lastPing = "16:30"
                    )
                ) { }
                Spacer(modifier = Modifier.size(8.dp))
                GroupMember(
                    state = GroupMemberItemStateWithLocations(
                        memberState = dummyMemberState,
                        locations = dummyLocations,
                        allowShowingDevices = true,
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        lastPing = "16:30",
                    )
                ) { }
                GroupMember(
                    state = GroupMemberItemStateWithLocations(
                        memberState = dummyMemberState,
                        locations = dummyLocations.take(1),
                        allowShowingDevices = false,
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        lastPing = "16:30",
                    )
                ) { }
            }
        }
    }
}

val dummyMemberState = GroupMemberState(
    groupId = "",
    memberId = "",
    userId = "",
    name = "User name",
    image = ImageModel.emoji(Emojis.robot),
    isCurrentUser = true,
    isVerified = true,
    canHaveExpandButton = true
)

val dummyLocations = listOf(
    GroupMemberItemLocationState(
        locationId = "",
        name = "Device 1",
        image = ImageModel.emoji(Emojis.lock),
        actualBatteryLevel = 20,
        adjustedBatteryLevel = 20,
        isBatteryCharging = false,
        speed = "35 km/h",
        elevation = "250m",
        lastPing = "16:30",
        status = MemberDeviceStatusType.ACTIVE,
        canEdit = true,
        currentDevice = true,
        networkType = NetworkType.G2,
        signalStrength = NetworkSignalStrength.GOOD,
        visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
    ),
    GroupMemberItemLocationState(
        locationId = "",
        name = "Device 2",
        image = ImageModel.emoji(Emojis.lock),
        actualBatteryLevel = 20,
        adjustedBatteryLevel = 20,
        isBatteryCharging = false,
        speed = "35 km/h",
        elevation = "250m",
        lastPing = null,
        status = MemberDeviceStatusType.GPS_OFF,
        canEdit = true,
        currentDevice = true,
        networkType = NetworkType.G4,
        signalStrength = NetworkSignalStrength.POOR,
        visibility = MemberLocationVisibilityType.VISIBLE_ONCE
    ),
    GroupMemberItemLocationState(
        locationId = "",
        name = "Device 3",
        image = ImageModel.emoji(Emojis.lock),
        actualBatteryLevel = 20,
        adjustedBatteryLevel = 20,
        isBatteryCharging = true,
        speed = "35 km/h",
        elevation = "250m",
        lastPing = "16:30",
        status = MemberDeviceStatusType.NOT_SHARING,
        canEdit = false,
        currentDevice = false,
        networkType = NetworkType.WIFI,
        signalStrength = NetworkSignalStrength.GOOD,
        visibility = MemberLocationVisibilityType.HIDDEN
    )
)