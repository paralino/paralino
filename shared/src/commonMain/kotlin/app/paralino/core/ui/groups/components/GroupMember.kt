package app.paralino.core.ui.groups.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.MemberAction

@Composable
fun GroupMember(
    state: GroupMemberItemState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    canShowOldLocation: Boolean = false,
    onActionClick: ((MemberAction) -> Unit)? = null,
    onDeleteDeviceClick: ((GroupMemberItemLocationState) -> Unit)? = null,
    onToggleLocation: ((GroupMemberItemStateWithLocations) -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
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
                GroupMemberWithLocations(
                    state = GroupMemberItemStateWithLocations(
                        memberState = state.memberState,
                        locations = listOfNotNull(state.location),
                        actionItems = state.actionItems,
                        allowShowingDevices = false,
                        lastPing = state.lastPing
                    ),
                    canShowOldLocation = canShowOldLocation,
                    forceExpandAction = forceExpandAction,
                    collapseOnActionClick = collapseOnActionClick,
                    onActionClick = onActionClick,
                    onDeleteDeviceClick = onDeleteDeviceClick,
                    onToggleLocation = onToggleLocation,
                    extraActions = extraActions
                )
            }

            is GroupMemberItemStateWithLocations -> {
                GroupMemberWithLocations(
                    state = state,
                    forceExpandAction = forceExpandAction,
                    collapseOnActionClick = collapseOnActionClick,
                    canShowOldLocation = canShowOldLocation,
                    onActionClick = onActionClick,
                    onDeleteDeviceClick = onDeleteDeviceClick,
                    onToggleLocation = onToggleLocation,
                    extraActions = extraActions
                )
            }
        }
    }
}
