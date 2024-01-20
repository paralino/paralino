package app.paralino.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwipeableState
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapSheetSelectedContentType
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MapHomeSheetContent(
    swipeableState: SwipeableState<BottomSheetPosition>,
    readyState: MapHomeScreenState.Ready?,
    members: List<GroupMemberItemState>,
    places: List<PlaceListItemData>,
    onGroupClick: () -> Unit,
    onMemberClick: (GroupMemberItemState) -> Unit,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onOpenPlaces: () -> Unit,
    onCreatePlace: () -> Unit,
    onWholeContentClick: () -> Unit,
    onExpandSheet: () -> Unit,
    onCollapseSheet: () -> Unit,
    onFocusOnMembersClick: () -> Unit,
    onFocusOnPlacesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onWholeContentClick()
            }
    ) {

        val contentAlpha by remember {
            derivedStateOf {
                if (swipeableState.progress.to == BottomSheetPosition.COLLAPSED) {
                    1f - swipeableState.progress.fraction
                } else if (swipeableState.progress.from == BottomSheetPosition.COLLAPSED) {
                    swipeableState.progress.fraction
                } else {
                    1f
                }
            }
        }

        Divider(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(percent = 50))
                .width(30.dp),
            thickness = 6.dp
        )

        if (readyState != null) {
            MapGroupNameAndMemberInfo(
                modifier = Modifier.padding(horizontal = 20.dp),
                groupName = readyState.groupName,
                groupImage = readyState.groupImage,
                userCount = readyState.userCount,
                activeUserCount = readyState.activeUserCount,
                inactiveUserCount = readyState.inactiveUserCount,
                idleUserCount = readyState.idleUserCount,
                verified = readyState.allMembersVerified,
                interactionVisibilityFraction = contentAlpha,
                onGroupClick = {
                    if (swipeableState.isCollapsed()) {
                        onExpandSheet()
                    } else {
                        onGroupClick()
                    }
                }
            )
            Spacer(modifier = Modifier.size(12.dp))
        }

        Row(
            modifier = Modifier
                .padding(start = 20.dp)
                .alpha(contentAlpha)
                .align(Alignment.Start)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapBottomSheetTab(
                modifier = Modifier,
                selected = readyState?.mapSheetSelectedContentType == MapSheetSelectedContentType.MEMBERS,
                focused = readyState?.mapHomeScreenFocusType?.areMembersFocused == true,
                text = MR.strings.group_details_section_members_title.localizedString(),
                icon = CoreAppIcons.Group.asPainterResource(),
                onClick = onFocusOnMembersClick
            )
            Spacer(modifier = Modifier.size(8.dp))
            MapBottomSheetTab(
                modifier = Modifier,
                selected = readyState?.mapSheetSelectedContentType == MapSheetSelectedContentType.PLACES,
                focused = readyState?.mapHomeScreenFocusType?.arePlacesFocused == true,
                text = MR.strings.places_list_screen_title.localizedString(),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                onClick = onFocusOnPlacesClick
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 80.dp)
        ) {
            when (readyState?.mapSheetSelectedContentType) {
                MapSheetSelectedContentType.PLACES -> {
                    placesList(
                        places = places,
                        onCreatePlace = onCreatePlace,
                        onOpenPlaces = onOpenPlaces,
                        onPlaceClick = onPlaceClick
                    )
                }

                MapSheetSelectedContentType.MEMBERS -> {
                    membersList(
                        members = members,
                        onMemberClick = onMemberClick
                    )
                }

                null -> {}
            }
        }
    }
}

private fun LazyListScope.membersList(
    members: List<GroupMemberItemState>,
    onMemberClick: (GroupMemberItemState) -> Unit
) {
    this.itemsIndexed(
        items = members,
        key = { index, item -> item.keyId }
    ) { index, itemState ->
        if (index > 0)
            Spacer(modifier = Modifier.size(8.dp))
        GroupMember(
            state = itemState,
            backgroundColor = Color.Transparent,
            collapseOnActionClick = true,
            forceExpandAction = false,
            onActionClick = null,
            onClick = { onMemberClick(itemState) }
        )
    }
}

private fun LazyListScope.placesList(
    places: List<PlaceListItemData>,
    onCreatePlace: () -> Unit,
    onOpenPlaces: () -> Unit,
    onPlaceClick: (PlaceListItemData) -> Unit
) {
    if (places.isEmpty()) {
        this.item {
            DefaultEmptyState(
                content = stringResource(MR.strings.places_list_screen_empty_info),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                buttonText = stringResource(MR.strings.places_create_button_create),
                buttonIcon = CoreAppIcons.Add.asPainterResource()
            ) {
                onCreatePlace()
            }
        }
    } else {
        this.itemsIndexed(
            items = places,
            key = { index, item -> item.id }
        ) { index, itemState ->
            if (index > 0)
                Spacer(modifier = Modifier.size(8.dp))
            PlaceListItem(
                state = itemState,
                hasEndArrow = false,
                backgroundColor = MaterialTheme.colors.surface,
                onClick = { onPlaceClick(itemState) }
            )
        }
        this.item(key = "places_list") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                FilledButton(
                    modifier = Modifier.padding(top = 20.dp),
                    style = ButtonStyle.SMALL,
                    icon = null,
                    elevation = ButtonDefaults.elevation(
                        0.dp,
                        0.dp,
                        0.dp,
                        0.dp,
                        0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                    onClick = onOpenPlaces
                ) {
                    Text(stringResource(MR.strings.button_showAll))
                    Icon(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(20.dp),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun MapBottomSheetTab(
    modifier: Modifier = Modifier,
    selected: Boolean,
    focused: Boolean,
    text: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onBackground
    )
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colors.primary
        else Color.Transparent
    )

    val focusedIndicatorColor by animateColorAsState(
        if (focused) AppColors.accent
        else contentColor.copy(alpha = 0.2f)
    )

    val focusedIndicatorContentColor by animateColorAsState(
        if (focused) AppColors.accent
        else Color.Transparent
    )

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(100))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(18.dp),
            painter = icon,
            tint = contentColor,
            contentDescription = null
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            modifier = Modifier,
            text = text,
            fontSize = 14.sp,
            color = contentColor
        )
        Spacer(modifier = Modifier.size(8.dp))
        AnimatedVisibility(
            visible = selected
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(
                        width = 1.dp,
                        color = focusedIndicatorColor,
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .background(
                        color = focusedIndicatorContentColor,
                        shape = CircleShape
                    )
            )
        }
    }
}