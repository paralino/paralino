package app.paralino.android.ui.groups.members

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import app.paralino.android.R
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.utils.extensions.fromUTCtoUserLastPing
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.*
import app.paralino.core.utils.Emojis
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

@Composable
fun GroupMember(
    state: GroupMemberItemState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    onActionClick: (() -> Unit)? = null,
    onDeleteDeviceClick: ((GroupMemberItemLocationState) -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        when (state) {
            is GroupMemberItemStateWithOneLocation -> {
                GroupMemberWithOneLocation(
                    state = state,
                    forceExpandAction = forceExpandAction,
                    collapseOnActionClick = collapseOnActionClick,
                    onActionClick = onActionClick,
                    extraActions = extraActions
                )
            }
            is GroupMemberItemStateWithLocations -> {
                GroupMemberWithLocations(
                    state = state,
                    forceExpandAction = forceExpandAction,
                    collapseOnActionClick = collapseOnActionClick,
                    onActionClick = onActionClick,
                    onDeleteDeviceClick = onDeleteDeviceClick,
                    extraActions = extraActions
                )
            }
        }
    }
}

@Composable
fun GroupMemberItemState.getLastPingText(): String? {

    val lastLocation = when (this) {
        is GroupMemberItemStateWithOneLocation -> {
            this.location
        }
        is GroupMemberItemStateWithLocations -> {
            this.locations.filter { it.lastPing != null }.minByOrNull { it.lastPing!! }
        }
        else -> null
    }

    return lastLocation?.getLastPingText()
}

@Composable
fun GroupMemberItemLocationState.getLastPingText(): String? {
    return lastPing?.toJavaLocalDateTime()?.let {
        if (it.isAfter(LocalDateTime.now().minusMinutes(2)))
            stringResource(R.string.group_members_last_active_status_now)
        else
            it.fromUTCtoUserLastPing()
    }
}

val MemberAction.actionIcon: ImageVector
    get() = when (this) {
        MemberAction.DELETE_MEMBER -> Icons.Rounded.PersonRemove
        MemberAction.DELETE_LOCATION -> Icons.Rounded.Delete
    }

val MemberAction.actionText: Int
    get() = when (this) {
        MemberAction.DELETE_MEMBER -> R.string.group_members_item_remove_button
        MemberAction.DELETE_LOCATION -> R.string.group_members_item_clear_location_button
    }

val GroupMemberStatus.actionText: Int?
    get() = when (this) {
        GroupMemberStatus.ACTIVE -> null
        GroupMemberStatus.INACTIVE -> R.string.group_members_status_inactive
    }

@Preview(device = Devices.PIXEL_3A)
@Composable
fun MemberPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(modifier = Modifier.padding(40.dp)) {
                GroupMember(
                    state = GroupMemberItemStateWithOneLocation(
                        memberState = dummyMemberState,
                        location = null
                    )
                ) { }
                Spacer(modifier = Modifier.size(8.dp))
                GroupMember(
                    state = GroupMemberItemStateWithOneLocation(
                        memberState = dummyMemberState,
                        location = dummyLocations.first()
                    )
                ) { }
                Spacer(modifier = Modifier.size(8.dp))
                GroupMember(
                    state = GroupMemberItemStateWithLocations(
                        memberState = dummyMemberState,
                        locations = dummyLocations
                    )
                ) { }
            }
        }
    }
}

val dummyMemberState = GroupMemberState(
    memberId = "",
    userId = "",
    actionItem = MemberAction.DELETE_MEMBER,
    name = "User name",
    image = ImageModel.emoji(Emojis.robot),
    isCurrentUser = true
)

val dummyLocations = listOf(
    GroupMemberItemLocationState(
        locationId = "",
        name = "Device 1",
        image = ImageModel.emoji(Emojis.lock),
        batterLevel = 20,
        speedMs = 10f,
        elevationMeters = 50f,
        lastPing = LocalDateTime.now().minusDays(2).toKotlinLocalDateTime(),
        status = GroupMemberStatus.ACTIVE,
        canEdit = true,
        currentDevice = true
    ),
    GroupMemberItemLocationState(
        locationId = "",
        name = "Device 2",
        image = ImageModel.emoji(Emojis.lock),
        batterLevel = 20,
        speedMs = null,
        elevationMeters = null,
        lastPing = null,
        status = GroupMemberStatus.INACTIVE,
        canEdit = true,
        currentDevice = true
    ),
    GroupMemberItemLocationState(
        locationId = "",
        name = "Device 3",
        image = ImageModel.emoji(Emojis.lock),
        batterLevel = 20,
        speedMs = 10f,
        elevationMeters = 50f,
        lastPing = LocalDateTime.now().minusDays(2).toKotlinLocalDateTime(),
        status = GroupMemberStatus.ACTIVE,
        canEdit = false,
        currentDevice = false
    )
)