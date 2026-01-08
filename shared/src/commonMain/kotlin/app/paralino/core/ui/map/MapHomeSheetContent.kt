package app.paralino.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val dividerHeight = 6.dp
private val dividerTopPadding = 8.dp
private val dividerBottomPadding = 6.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MapHomeSheetContent(
    modifier: Modifier = Modifier,
    screenPaddingValues: PaddingValues,
    swipeableState: SwipeableSheetState,
    readyState: MapHomeScreenState.Ready,
    members: List<GroupMemberItemState>,
    places: List<PlaceListItemData>,
    onMapGroupInfoHeightChange: (Float) -> Unit,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
    onWholeContentClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(swipeableState) {
        snapshotFlow { swipeableState.settledValue }
            .distinctUntilChanged()
            .collectLatest {
                if (it >= BottomSheetPosition.HALF_EXPANDED) {
                    scope.launch {
                        swipeableState.mapContentListState.scrollToItem(0, 0)
                    }
                }
            }
    }

    Column(
        modifier = modifier
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
               1f - swipeableState
                   .progress(from = BottomSheetPosition.HALF_EXPANDED, to = BottomSheetPosition.COLLAPSED)
                   .times(1.2f)
                   .coerceIn(0f, 1f)
            }
        }

        if (readyState.hasPersonalSpaceIntroBanner) {
            PersonalSpaceFeatureIntroContent(
                modifier = modifier,
                onEvent = onEvent,
            )
        }

        Divider(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    top = dividerTopPadding,
                    bottom = 0.dp //animateDpAsState(dividerBottomPadding.value.minus((1f - contentAlpha) * dividerBottomPadding.value).dp).value
                )
//                .padding(top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .width(30.dp),
            thickness = dividerHeight
        )

        if (readyState != null) {
            MapGroupNameAndMemberInfo(
                modifier = Modifier
                    .padding(start = 14.dp, end = 16.dp)
                    .onGloballyPositioned { coordinates ->
                        val height = with(density) {
                            screenPaddingValues.calculateBottomPadding()
                                .plus(SwipeableSheetState.sheetBottomCollapsedPadding)
                                .plus(dividerTopPadding.times(2) + dividerHeight.times(2))
                                .toPx()
                        }
                        onMapGroupInfoHeightChange(coordinates.size.height.plus(height))
                    },
                isPersonalSpace = readyState.isPersonalSpace,
                groupName = readyState.groupOrSpaceName,
                groupImage = readyState.groupOrSpaceImage,
                userCount = readyState.userCount,
                placesCount = places.size,
                activeUserCount = readyState.activeUserCount,
                inactiveUserCount = readyState.inactiveUserCount,
                idleUserCount = readyState.idleUserCount,
                verified = readyState.allMembersVerified,
                interactionVisibilityFraction = contentAlpha,
                onGroupContentClick = onWholeContentClick,
                onGroupDetailButtonClick = {
                    if (swipeableState.isCollapsed()) {
                        onWholeContentClick()
                    } else {
                        onEvent(MapHomeScreenUiEvent.OnGroupDetailsClick)
                    }
                }
            )

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .alpha(contentAlpha)
                    .align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (readyState.isPersonalSpace) {
                    MapBottomSheetTab(
                        modifier = Modifier,
                        selected = readyState?.mapSheetSelectedContentType == MapSheetSelectedContentType.MEMBERS,
                        focused = readyState?.mapHomeScreenFocusType?.areMembersFocused == true,
                        text = MR.strings.group_member_devices_section_title.localizedString(),
                        icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
                        onClick = {
                            onEvent(MapHomeScreenUiEvent.OnFocusOnMembersClick)
                        }
                    )
                } else {
                    MapBottomSheetTab(
                        modifier = Modifier,
                        selected = readyState?.mapSheetSelectedContentType == MapSheetSelectedContentType.MEMBERS,
                        focused = readyState?.mapHomeScreenFocusType?.areMembersFocused == true,
                        text = MR.strings.group_details_section_members_title.localizedString(),
                        icon = CoreAppIcons.Group.asPainterResource(),
                        onClick = {
                            onEvent(MapHomeScreenUiEvent.OnFocusOnMembersClick)
                        }
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                MapBottomSheetTab(
                    modifier = Modifier,
                    selected = readyState?.mapSheetSelectedContentType == MapSheetSelectedContentType.PLACES,
                    focused = readyState?.mapHomeScreenFocusType?.arePlacesFocused == true,
                    text = MR.strings.places_list_screen_title.localizedString(),
                    icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                    onClick = {
                        onEvent(MapHomeScreenUiEvent.OnFocusOnPlacesClick)
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(top = 2.dp)
                .weight(1f)
                .alpha(contentAlpha),
            state = swipeableState.mapContentListState,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 0.dp,
                bottom = swipeableState.contentBottomPadding
            )
        ) {
            this.stickyHeader(key = "top_gradient") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colors.background,
                                    MaterialTheme.colors.background,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            when (readyState?.mapSheetSelectedContentType) {
                MapSheetSelectedContentType.PLACES -> {
                    placesList(
                        isPersonalSpace = readyState.isPersonalSpace,
                        places = places,
                        onCreatePlace = {
                            onEvent(MapHomeScreenUiEvent.OnOpenCreatePlaceScreen(location = null))
                        },
                        onOpenPlaces = {
                            onEvent(MapHomeScreenUiEvent.OnOpenPlacesScreen)
                        },
                        onPlaceClick = {
                            onEvent(MapHomeScreenUiEvent.OnPlaceClick(it))
                        }
                    )
                }

                MapSheetSelectedContentType.MEMBERS -> {
                    membersList(
                        members = members,
                        onEvent = {
                            onEvent(MapHomeScreenUiEvent.OnMemberItemEvent(it))
                        }
                    )
                }

                null -> {}
            }
        }
    }
}

private fun LazyListScope.membersList(
    members: List<GroupMemberItemState>,
    onEvent: (GroupMemberItemEvent) -> Unit
) {
    this.itemsIndexed(
        items = members,
        key = { index, item -> item.keyId }
    ) { index, itemState ->
        if (index > 0)
            Spacer(modifier = Modifier.size(8.dp))
        GroupMember(
            state = itemState,
            collapseOnActionClick = true,
            forceExpandAction = itemState.locations.size > 1,
            onEvent = onEvent
        )
    }
}

private fun LazyListScope.placesList(
    isPersonalSpace: Boolean,
    places: List<PlaceListItemData>,
    onCreatePlace: () -> Unit,
    onOpenPlaces: () -> Unit,
    onPlaceClick: (PlaceListItemData) -> Unit
) {
    if (places.isEmpty()) {
        this.item {
            DefaultEmptyState(
                content = if (isPersonalSpace) {
                    stringResource(MR.strings.places_list_screen_empty_info_personal)
                } else {
                    stringResource(MR.strings.places_list_screen_empty_info_group)
                },
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
                    elevation = 0.dp,
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
private fun MapBottomSheetTab(
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
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(100))
            .background(backgroundColor)
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
            color = contentColor,
            style = MaterialTheme.typography.button
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
