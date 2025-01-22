package app.paralino.core.ui.groups.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.utils.Emojis

@Composable
fun GroupMember(
    state: GroupMemberItemState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    canShowOldLocation: Boolean = false,
    onEvent: (GroupMemberItemEvent) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEvent(GroupMemberItemEvent.onItemClick(state)) }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        when (state) {
            is GroupMemberItemStateWithOneLocation -> {
                GroupMemberWithLocations(
                    state = GroupMemberItemStateWithLocations(
                        memberState = state.memberState,
                        locations = listOfNotNull(state.location),
                        actions = state.actions,
                        allowShowingDevices = false,
                        lastLocationPing = state.lastLocationPing,
                        lastStatusPing = state.lastStatusPing
                    ),
                    canShowOldLocation = canShowOldLocation,
                    forceExpandAction = forceExpandAction,
                    collapseOnActionClick = collapseOnActionClick,
                    onEvent = onEvent
                )
            }

            is GroupMemberItemStateWithLocations -> {
                GroupMemberWithLocations(
                    state = state,
                    forceExpandAction = forceExpandAction,
                    collapseOnActionClick = collapseOnActionClick,
                    canShowOldLocation = canShowOldLocation,
                    onEvent = onEvent
                )
            }
        }
    }
}

val dummyMemberState = GroupMemberState(
    groupId = "",
    memberId = "",
    userId = "",
    name = "Peter",
    image = ImageModel.emoji(Emojis.robot),
    paralinoId = null,
    isCurrentUser = true,
    isVerified = true,
    canHaveExpandButton = false
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
        lastLocationPing = "16:30",
        lastStatusPing = "16:30",
        accuracyMeters = 400f,
        deviceStatus = MemberDeviceStatusType.ACTIVE,
        canEdit = true,
        currentDevice = true,
        onlineStatus = MemberDeviceOnlineStatusType.ONLINE,
        networkType = NetworkType.G2,
        signalStrength = NetworkSignalStrength.GOOD,
        visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS
    ),
    GroupMemberItemLocationState(
        locationId = "",
        name = "Primary",
        image = ImageModel.emoji(Emojis.lock),
        actualBatteryLevel = 34,
        adjustedBatteryLevel = 20,
        isBatteryCharging = false,
        speed = "35 km/h",
        elevation = "250m",
        lastLocationPing = null,
        lastStatusPing = "16:30",
        accuracyMeters = 20f,
        deviceStatus = MemberDeviceStatusType.ACTIVE,
        canEdit = true,
        currentDevice = true,
        onlineStatus = MemberDeviceOnlineStatusType.ONLINE,
        networkType = NetworkType.G3,
        signalStrength = NetworkSignalStrength.GOOD,
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
        lastLocationPing = "16:30",
        lastStatusPing = "16:30",
        accuracyMeters = null,
        deviceStatus = MemberDeviceStatusType.NOT_SHARING,
        canEdit = false,
        currentDevice = false,
        onlineStatus = MemberDeviceOnlineStatusType.BUSY,
        networkType = NetworkType.WIFI,
        signalStrength = NetworkSignalStrength.GOOD,
        visibility = MemberLocationVisibilityType.HIDDEN
    )
)