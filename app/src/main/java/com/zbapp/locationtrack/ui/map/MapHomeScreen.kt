package com.zbapp.locationtrack.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.GroupWork
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.asFlow
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.presentation.groups.GroupMemberItemState
import com.zbapp.core.presentation.groups.GroupMemberStatus
import com.zbapp.core.presentation.groups.MemberAction
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.groups.members.GroupMember
import com.zbapp.locationtrack.ui.theme.AppColors
import com.zbapp.locationtrack.utils.extensions.toLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MapHomeScreen(
    appState: MainAppState,
    groupId: String?,
    vm: MapViewModel = getViewModel(),
) {
//    val mapViewState = rememberMapHomeStoreLocalState()
    var trackGroup by remember { mutableStateOf(false) }
    var trackMarkerId: String? by remember { mutableStateOf(null) }
    val sharingTime: String? by vm.getSharingTime().asFlow().collectAsState(initial = null)
    val users by vm.getMarkers().asFlow().collectAsState(initial = Payload.Loading)

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {

        val (map, groupSelect, groupTrack, userList, shareTime) = createRefs()

        // map
//        AndroidView(
//            modifier = Modifier.constrainAs(map) {
//                linkTo(
//                    start = parent.start,
//                    end = parent.end,
//                    top = parent.top,
//                    bottom = parent.bottom
//                )
//                width = Dimension.fillToConstraints
//                height = Dimension.fillToConstraints
//            },
//            factory = {
//                mapViewState.initMap()
//                mapViewState.initMapListeners(
//                    onMapTouch = {
//                        trackGroup = false
//                    },
//                    onMarkerClick = {
//                        true
//                    }
//                )
//                mapViewState.mapView
//            },
//            update = {
//
//            }
//        )

        // group picker button
        MapGroupPicker(
            modifier = Modifier.constrainAs(groupSelect) {
                top.linkTo(parent.top, margin = 8.dp)
                centerHorizontallyTo(parent)
            },
            groupName = "Robots in da house",
            groupImage = ImageModel.emoji(Emojis.robot),
            userCount = 5,
            inactiveUserCount = 2
        ) {
            // TODO open group picker
        }

        // sharing time info
        AnimatedVisibility(
            visible = sharingTime != null,
            modifier = Modifier
                .constrainAs(shareTime) {
                    start.linkTo(parent.start, margin = 8.dp)
                    bottom.linkTo(userList.top, margin = 8.dp)
                }
        ) {
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colors.secondary,
                        MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 2.dp)
                    .padding(start = 4.dp, end = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = sharingTime ?: "",
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp)
                )
            }
        }

        // activate group tracking button
        FloatingActionButton(
            modifier = Modifier
                .constrainAs(groupTrack) {
                    end.linkTo(parent.end, margin = 8.dp)
                    bottom.linkTo(userList.top, margin = 16.dp)
                }
                .size(30.dp),
            onClick = { trackGroup = !trackGroup },
            backgroundColor = if (trackGroup) AppColors.accent else MaterialTheme.colors.surface
        ) {
            Icon(
                imageVector = Icons.Rounded.GroupWork,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (trackGroup) AppColors.darkThemePrimary else MaterialTheme.colors.secondary
            )
        }

        // user list
        Box(
            modifier = Modifier.constrainAs(userList) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        ) {
            AnimatedVisibility(visible = trackMarkerId != null) {
                // TODO move to proper state
                HorizontalPager(
                    count = users.data?.memberListData?.size ?: 0,
                    key = { users.data?.memberListData?.get(it)?.member?.id!! }
                ) {
                    val member = users.data?.memberListData?.get(it)!!
                    val memberState = GroupMemberItemState(
                        actionItem = if (member.member.userId == config.userId) MemberAction.DELETE_LOCATION else null,
                        name = member.member.name,
                        image = member.member.image,
                        batterLevel = member.member.mutableData?.batteryLevel,
                        lastPing = member.member.mutableData?.timeUTC?.toLocalDateTime()
                            ?.toKotlinLocalDateTime(),
                        status = if (member.member.isActive) GroupMemberStatus.ACTIVE else GroupMemberStatus.INACTIVE
                    )
                    GroupMember(state = memberState) { }
                }
            }
        }
    }
}

class MapHomeScreenLocalState(
    val selectedGroup: UserGroup,
    val trackPositions: Boolean,
    val trackMarkerId: String?,
    val trackGroupId: String?,
)
