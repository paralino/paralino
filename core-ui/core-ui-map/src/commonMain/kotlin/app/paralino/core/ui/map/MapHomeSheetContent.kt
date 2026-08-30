package app.paralino.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
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
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.GroupMemberItemEvent
import app.paralino.core.presentation.common.GroupMemberItemState
import app.paralino.core.presentation.common.PlaceListItemData
import app.paralino.core.presentation.map.BottomSheetPosition
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapSheetSelectedContentType
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.groups.components.GroupMemberActionButton
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.ui.theme.LocalScreenDimensionType
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.dashedDotDashedBorder
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object MapHomeSheetContentSizes {
    val dividerHeight = 6.dp
    val dividerTopPadding = 8.dp
    val collapsedDividerTopPadding = 12.dp
    val dividerBottomPadding = 6.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MapHomeSheetContent(
    modifier: Modifier = Modifier,
    screenPaddingValues: PaddingValues,
    swipeableState: SwipeableSheetState,
    readyState: MapHomeScreenState.Ready,
    members: List<GroupMemberItemState>,
    places: List<PlaceListItemData>,
    privacyZones: List<PlaceListItemData>,
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
                if (it.order >= BottomSheetPosition.HALF_EXPANDED.order) {
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

        val contentCollapsedFraction by remember {
            derivedStateOf {
                swipeableState
                    .progress(from = BottomSheetPosition.COLLAPSED_MINIMISED, to = BottomSheetPosition.COLLAPSED)
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
                    top = MapHomeSheetContentSizes.dividerTopPadding + MapHomeSheetContentSizes.collapsedDividerTopPadding.times(1f - contentCollapsedFraction),
                    bottom = 0.dp //animateDpAsState(dividerBottomPadding.value.minus((1f - contentAlpha) * dividerBottomPadding.value).dp).value
                )
//                .padding(top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .width(30.dp),
            thickness = MapHomeSheetContentSizes.dividerHeight
        )

        MapGroupNameAndMemberInfo(
            modifier = Modifier
                .padding(start = 14.dp, end = 16.dp)
                .onGloballyPositioned { coordinates ->
                    val extraHeightDp = screenPaddingValues.calculateBottomPadding()
                        .plus(SwipeableSheetState.sheetBottomCollapsedPadding)
                        .plus(MapHomeSheetContentSizes.dividerTopPadding.times(2) + MapHomeSheetContentSizes.dividerHeight.times(2))

                    val extraHeightPx = with(density) {
                        extraHeightDp.toPx()
                    }
                    onMapGroupInfoHeightChange(coordinates.size.height.plus(extraHeightPx))
                },
            isPersonalSpace = readyState.isPersonalSpace,
            groupName = readyState.groupOrSpaceName,
            groupImage = readyState.groupOrSpaceImage,
            userCount = readyState.userCount,
            placesCount = places.size,
            activeUserCount = readyState.activeUserCount,
            inactiveUserCount = readyState.inactiveUserCount,
            idleUserCount = readyState.idleUserCount,
            hasWarning = !readyState.allMembersVerified || readyState.errors.isNotEmpty(),
            interactionVisibilityFraction = contentAlpha,
            collapsedVisibilityFraction = contentCollapsedFraction,
            onGroupContentClick = onWholeContentClick,
            onGroupDetailButtonClick = {
                if (swipeableState.isCollapsed() || swipeableState.isCollapsedAndMinimised()) {
                    onWholeContentClick()
                } else {
                    onEvent(MapHomeScreenUiEvent.OnGroupDetailsClick)
                }
            }
        )

        val screenSize = LocalScreenDimensionType.current
        val bottomContentPadding = remember(screenSize, screenPaddingValues) {
            if (screenSize.isSmall) {
                (screenSize.maxHeightDp * 0.2f) + screenPaddingValues.calculateBottomPadding() + screenPaddingValues.calculateTopPadding()
            } else {
                (screenSize.maxHeightDp * 0.17f) + screenPaddingValues.calculateBottomPadding()
            }
        }

        if (!screenSize.isSmall) {
            SheetTabs(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .align(Alignment.Start),
                readyState = readyState,
                contentAlpha = contentAlpha,
                onEvent = onEvent
            )
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
                bottom = bottomContentPadding
            )
        ) {
            this.stickyHeader(key = "top_gradient") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (screenSize.isSmall) 8.dp else 10.dp)
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

            if (screenSize.isSmall) {
                this.item(key = "sheet_tabs") {
                    SheetTabs(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .horizontalScroll(rememberScrollState())
                            .align(Alignment.Start),
                        readyState = readyState,
                        contentAlpha = contentAlpha,
                        onEvent = onEvent
                    )
                }
            }

            this.items(readyState.errors, key = { "error_$it" }) {
                ConditionErrorItem(data = it) {
                    onEvent(MapHomeScreenUiEvent.OnConditionErrorItemClick(it))
                }
            }

            if (readyState.errors.isNotEmpty()) {
                this.item { Spacer(modifier = Modifier.size(10.dp)) }
            }

            when (readyState.mapSheetSelectedContentType) {
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

                MapSheetSelectedContentType.PRIVACY_ZONES -> {
                    privacyZonesList(
                        zones = privacyZones,
                        onCreateZone = {
                            onEvent(MapHomeScreenUiEvent.OnOpenCreatePrivacyZoneScreen(location = null))
                        },
                        onOpenZones = {
                            onEvent(MapHomeScreenUiEvent.OnOpenPrivacyZonesScreen)
                        },
                        onZoneClick = {
                            onEvent(MapHomeScreenUiEvent.OnPrivacyZoneClick(it))
                        }
                    )
                }

                MapSheetSelectedContentType.MEMBERS -> {
                    membersList(
                        canStopTracking = readyState.personalSpaceSharingContract != null && readyState.isPersonalSpace,
                        canStopSharing = readyState.selectedGroupSharingContract != null && !readyState.isPersonalSpace,
                        canAddNewMember = !readyState.isPersonalSpace && readyState.isCurrentUserOwnerOfSelectedGroup,
                        hasUpgradeBanner = !readyState.groupPlanConfig.hasOngoingRealtime && (readyState.isPersonalSpace || readyState.isCurrentUserOwnerOfSelectedGroup),
                        members = members,
                        memberEvent = {
                            onEvent(MapHomeScreenUiEvent.OnMemberItemEvent(it))
                        },
                        onAddNewMember = {
                            onEvent(MapHomeScreenUiEvent.OnOpenMemberInviteScreen)
                        },
                        onOpenSettings = {
                            onEvent(MapHomeScreenUiEvent.OpenGroupDetails(readyState.groupOrSpaceId))
                        },
                        onBannerTap = {
                            onEvent(MapHomeScreenUiEvent.OpenPaywall(type = BillingScreenSourceType.UPGRADE_MAP_SHEET))
                        },
                        stopLocationSharing = {
                            onEvent(MapHomeScreenUiEvent.StopLocationSharing(groupOrSpaceId = readyState.groupOrSpaceId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTabs(
    modifier: Modifier = Modifier,
    readyState: MapHomeScreenState.Ready,
    contentAlpha: Float,
    onEvent: (MapHomeScreenUiEvent) -> Unit
) {
    Row(
        modifier = modifier
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (readyState.isPersonalSpace) {
            MapBottomSheetTab(
                modifier = Modifier,
                selected = readyState.mapSheetSelectedContentType == MapSheetSelectedContentType.MEMBERS,
                focused = readyState.mapHomeScreenFocusType.areMembersFocused,
                text = MR.strings.group_member_devices_section_title.localizedString(),
                icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
                onClick = {
                    onEvent(MapHomeScreenUiEvent.OnFocusOnMembersClick)
                }
            )
        } else {
            MapBottomSheetTab(
                modifier = Modifier,
                selected = readyState.mapSheetSelectedContentType == MapSheetSelectedContentType.MEMBERS,
                focused = readyState.mapHomeScreenFocusType.areMembersFocused,
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
            selected = readyState.mapSheetSelectedContentType == MapSheetSelectedContentType.PLACES,
            focused = readyState.mapHomeScreenFocusType.arePlacesFocused,
            text = MR.strings.places_list_screen_title.localizedString(),
            icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
            onClick = {
                onEvent(MapHomeScreenUiEvent.OnFocusOnPlacesClick)
            }
        )

        if (readyState.privacyZones.isNotEmpty()) {
            Spacer(modifier = Modifier.size(8.dp))
            MapBottomSheetTab(
                modifier = Modifier,
                selected = readyState.mapSheetSelectedContentType == MapSheetSelectedContentType.PRIVACY_ZONES,
                focused = readyState.mapHomeScreenFocusType.arePrivacyZonesFocused,
                text = MR.strings.privacy_zones_list_screen_title.localizedString(),
                icon = CoreAppIcons.PrivacyZone.asPainterResource(),
                onClick = {
                    onEvent(MapHomeScreenUiEvent.OnFocusOnPrivacyZonesClick)
                }
            )
        }
    }
}

private fun LazyListScope.membersList(
    canStopSharing: Boolean,
    canStopTracking: Boolean,
    canAddNewMember: Boolean,
    hasUpgradeBanner: Boolean,
    members: List<GroupMemberItemState>,
    memberEvent: (GroupMemberItemEvent) -> Unit,
    onAddNewMember: () -> Unit,
    onBannerTap: () -> Unit,
    onOpenSettings: () -> Unit,
    stopLocationSharing: () -> Unit,
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
            onEvent = memberEvent
        )
    }

    if (members.size == 1 && canAddNewMember) {
        this.item("add_member_button") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                GroupMemberActionButton(
                    modifier = Modifier,
                    startIcon = CoreAppIcons.PersonAdd.asPainterResource(),
                    endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                    text = stringResource(MR.strings.invite_connect_screen_title),
                    onClick = onAddNewMember
                )
            }
        }
    }

    if (hasUpgradeBanner) {
        this.item("premium_info") {
            DataInfoChip(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .dashedDotDashedBorder(
                        width = 1.dp,
                        color = if (isSystemInDarkTheme()) {
                            AppColors.accentDark
                        } else {
                            AppColors.accentLight
                        },
                        shape = RoundedCornerShape(10.dp),
                        dashLength = 6.dp
                    ),
                shape = RoundedCornerShape(10.dp),
                imageOrIcon = {
                    ImageModelImage(
                        imageModel = ImageModel.emoji(Emojis.lightBulb),
                        modifier = Modifier.size(16.dp),
                        useBackgroundForEmoji = false,
                        clipShape = CircleShape
                    )
                    Spacer(Modifier.size(4.dp))
                },
                text = {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(MR.strings.plans_banner_inline_realtime_location),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                    )
                },
                endSection = {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (isSystemInDarkTheme()) {
                                    AppColors.accent.copy(alpha = 0.8f)
                                } else {
                                    AppColors.accentDark.copy(alpha = 0.2f)
                                },
                                shape = CircleShape
                            )
                            .padding(2.dp),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                    )
                },
                backgroundColor = AppColors.accentLight.copy(0.1f),
                onClick = onBannerTap
            )
        }
    }

    this.item(key = "settings_divider") {
        Divider(
            modifier = Modifier
                .padding(top = 20.dp)
                .padding(horizontal = 20.dp),
            color = Color.Gray.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }

    this.item(key = "settings_row") {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GroupMemberActionButton(
                modifier = Modifier,
                startIcon = CoreAppIcons.Settings.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.dashboard_groups_manage_button),
                onClick = onOpenSettings
            )

            if (canStopSharing) {
                GroupMemberActionButton(
                    modifier = Modifier,
                    startIcon = CoreAppIcons.StopCircle.asPainterResource(),
                    endIcon = null,
                    text = stringResource(MR.strings.location_sharing_button_stop_groups),
                    backgroundColor = AppColors.errorBackground,
                    contentColor = AppColors.redMain,
                    onClick = stopLocationSharing
                )
            }

            if (canStopTracking) {
                GroupMemberActionButton(
                    modifier = Modifier,
                    startIcon = CoreAppIcons.StopCircle.asPainterResource(),
                    endIcon = null,
                    text = stringResource(MR.strings.location_sharing_button_stop_personal),
                    backgroundColor = AppColors.errorBackground,
                    contentColor = AppColors.redMain,
                    onClick = stopLocationSharing
                )
            }

            if (members.size > 1 && canAddNewMember) {
                GroupMemberActionButton(
                    modifier = Modifier,
                    startIcon = CoreAppIcons.PersonAdd.asPainterResource(),
                    endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                    text = stringResource(MR.strings.invite_connect_screen_title),
                    onClick = onAddNewMember
                )
            }
        }
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
            AddNewActionButton(
                modifier = Modifier,
                text = stringResource(MR.strings.button_showAll),
                action = onOpenPlaces
            )
        }
    }
}

private fun LazyListScope.privacyZonesList(
    zones: List<PlaceListItemData>,
    onCreateZone: () -> Unit,
    onOpenZones: () -> Unit,
    onZoneClick: (PlaceListItemData) -> Unit
) {
    if (zones.isEmpty()) {
        this.item {
            DefaultEmptyState(
                content = stringResource(MR.strings.privacy_zones_empty_state_info),
                icon = CoreAppIcons.PrivacyZone.asPainterResource(),
                buttonText = stringResource(MR.strings.privacy_zones_create_button_create),
                buttonIcon = CoreAppIcons.Add.asPainterResource()
            ) {
                onCreateZone()
            }
        }
    } else {
        this.itemsIndexed(
            items = zones,
            key = { index, item -> item.id }
        ) { index, itemState ->
            if (index > 0)
                Spacer(modifier = Modifier.size(8.dp))
            PlaceListItem(
                state = itemState,
                hasEndArrow = false,
                backgroundColor = MaterialTheme.colors.surface,
                onClick = { onZoneClick(itemState) }
            )
        }
        this.item(key = "zones_list") {
            AddNewActionButton(
                modifier = Modifier,
                text = stringResource(MR.strings.button_showAll),
                action = onOpenZones
            )
        }
    }
}


@Composable
private fun AddNewActionButton(
    modifier: Modifier = Modifier,
    text: String,
    action: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        FilledButton(
            modifier = Modifier.padding(top = 20.dp),
            style = ButtonStyle.SMALL,
            icon = null,
            elevation = 0.dp,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
            onClick = action
        ) {
            Text(text)
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
