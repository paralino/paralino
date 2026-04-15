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
import app.paralino.core.presentation.common.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.common.MemberActionItemState
import app.paralino.core.presentation.common.MemberActionType
import app.paralino.core.presentation.common.MemberActionsState
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.groups.components.dummyLocations
import app.paralino.core.ui.groups.components.dummyMemberState
import app.paralino.core.ui.theme.AppTheme

@Preview(device = Devices.PIXEL_3A)
@Composable
fun GroupMemberPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(modifier = Modifier.padding(40.dp)) {
                GroupMember(
                    state = GroupMemberItemStateWithOneLocation(
                        memberState = dummyMemberState,
                        singleLocation = null,
                        deviceId = "",
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        isHighlighted = true,
                        isCollapsedOrMinimized = true
                    )
                ) { }
                Spacer(modifier = Modifier.size(8.dp))
                GroupMember(
                    state = GroupMemberItemStateWithOneLocation(
                        memberState = dummyMemberState.copy(isVerified = false),
                        singleLocation = dummyLocations.first(),
                        deviceId = "",
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        isHighlighted = true,
                        isCollapsedOrMinimized = false
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
                        lastLocationPing = "16:30",
                        lastStatusPing = "16:30",
                    )
                ) { }
                GroupMember(
                    state = GroupMemberItemStateWithLocations(
                        memberState = dummyMemberState,
                        locations = listOf(dummyLocations.get(1)),
                        allowShowingDevices = false,
                        actions = MemberActionsState(
                            deleteMember = MemberActionItemState(
                                MemberActionType.DELETE_MEMBER
                            )
                        ),
                        lastLocationPing = "16:30",
                        lastStatusPing = "16:30",
                    )
                ) { }
            }
        }
    }
}
