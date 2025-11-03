package app.paralino.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.asReady
import app.paralino.core.provider.current
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.groups.components.GroupMemberActionButton
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun MapHomeScreenElements(
    modifier: Modifier = Modifier,
    state: MapHomeScreenState,
    screenPaddingValues: PaddingValues,
    containerHeight: Dp,
    bottomOffset: Dp,
    sheetFullProgress: Float,
    isSheetHidden: Boolean,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
    bottomCards: @Composable BoxScope.(sheetPeekHeight: Dp) -> Unit
) {

    val sheetPeekHeight = (containerHeight - bottomOffset).coerceIn(
        minimumValue = when (DeviceType.current) {
            DeviceType.Android -> screenPaddingValues.calculateBottomPadding()
            DeviceType.iOS -> {
                if (isSheetHidden) screenPaddingValues.calculateBottomPadding() else 0.dp
            }
        },
        maximumValue = containerHeight * BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
    )

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {

        val readyState = rememberUpdatedState(state.asReady()).value

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenPaddingValues.calculateTopPadding())
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colors.background.copy(alpha = 0.6f),
                            MaterialTheme.colors.background.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // group picker button
            MapGroupOrSpacePickerTabs(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f, fill = false),
                isPersonalSpace = state.isPersonalSpace,
                groupImage = state.selectedGroupImage,
                spaceImage = state.currentUserImage,
                onGroupItemClick = {
                    onEvent(MapHomeScreenUiEvent.SwitchToGroups)
                },
                onGroupPickerClick = {
                    onEvent(MapHomeScreenUiEvent.OnGroupPickerClick)
                },
                onSpaceItemClick = {
                    onEvent(MapHomeScreenUiEvent.SwitchToPersonalSpace)
                }
            )

            FilledButton(
                modifier = Modifier.padding(end = 8.dp).size(42.dp),
                onClick = { onEvent(MapHomeScreenUiEvent.OnSettingsClick) },
                icon = null,
                style = ButtonStyle.ICON,
                elevation = 8.dp,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface
                ),
                content = {
                    Icon(
                        painter = CoreAppIcons.Settings.asPainterResource(),
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colors.surface,
                                shape = CircleShape
                            )
                            .padding(8.dp)
                    )
                }
            )
        }

        // map mode
        if (readyState != null) {
            Image(
                painter = if (!state.isSatelliteMode) {
                    MR.images.image_map_type_satellite.asPainterResource()
                } else if (isSystemInDarkTheme()) {
                    MR.images.image_map_type_default_dark.asPainterResource()
                } else {
                    MR.images.image_map_type_default_light.asPainterResource()
                },
                contentDescription = null,
                modifier = Modifier
                    .align(
                        when (state.deviceType) {
                            DeviceType.Android -> Alignment.End
                            DeviceType.iOS -> Alignment.Start
                        }
                    )
                    .padding(top = 14.dp, end = 8.dp, start = 8.dp)
                    .size(40.dp)
                    .clickable { onEvent(MapHomeScreenUiEvent.OnMapModeSwitch) }
                    .background(
                        color = AppColors.accentDark,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = sheetPeekHeight),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                MapHomeElementSharingRow(
                    sheetFullProgress = sheetFullProgress,
                    readyState = readyState,
                    onEvent = onEvent
                )
            }

            bottomCards(sheetPeekHeight)
        }
    }
}

@Composable
fun BoxScope.MapHomeElementMainCards(
    modifier: Modifier = Modifier,
    readyState: MapHomeScreenState.Ready?,
    screenPaddingValues: PaddingValues,
    sheetPeekHeight: Dp,
    cardHeight: (Dp) -> Unit,
    onEvent: (MapHomeScreenUiEvent) -> Unit
) {
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .padding(bottom = animateDpAsState(targetValue = sheetPeekHeight).value)
            .onGloballyPositioned { coordinates ->
                val height = with(density) { coordinates.size.height.toDp() - 20.dp }
                cardHeight(height)
            }
    ) {
        // focused item
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            visible = readyState != null && readyState.mapHomeScreenFocusType is MapHomeScreenFocusType.PLACE,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            readyState?.focusedPlace?.let { placeState ->
                Box(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    PlaceListItem(
                        modifier = Modifier.padding(top = 38.dp),
                        state = placeState,
                        hasEndArrow = true,
                        onClick = { onEvent(MapHomeScreenUiEvent.OnPlaceClick(placeState)) }
                    )
                    CloseFocusButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 8.dp),
                        onClick = {
                            onEvent(MapHomeScreenUiEvent.OnCloseFocusedItemClick)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            visible = readyState != null && readyState.mapHomeScreenFocusType is MapHomeScreenFocusType.MEMBER,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            readyState?.focusedMember?.let { memberState ->
                Box(
                    modifier = Modifier.padding(
                        bottom = when (DeviceType.current) {
                            DeviceType.Android -> 8.dp
                            DeviceType.iOS -> {
                                if (screenPaddingValues.calculateBottomPadding().value == 0f) {
                                    8.dp
                                } else {
                                    0.dp
                                }
                            }
                        }
                    )
                ) {
                    GroupMember(
                        modifier = Modifier.padding(top = 38.dp),
                        state = memberState,
                        backgroundColor = MaterialTheme.colors.background,
                        shape = RoundedCornerShape(16.dp),
                        collapseOnActionClick = false,
                        forceExpandAction = true,
                        canShowOldLocation = true,
                        onEvent = { event ->
                            onEvent(MapHomeScreenUiEvent.OnMemberItemEvent(event = event))
                        }
                    )
                    CloseFocusButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp),
                        onClick = {
                            onEvent(MapHomeScreenUiEvent.OnCloseFocusedItemClick)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapHomeElementSharingRow(
    sheetFullProgress: Float,
    readyState: MapHomeScreenState.Ready?,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    val sidePadding = 12.dp
        .times(
            1f - when {
                sheetFullProgress >= BottomSheetPosition.HALF_EXPANDED.screenHeightPercent -> 1f
                else -> sheetFullProgress / BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
            }
        )
        .plus(16.dp)

    val isMemberOrPlaceFocused =
        readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true || readyState?.locationHistoryMode != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = sidePadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isMemberOrPlaceFocused && readyState?.isPersonalSpace == true) {
            GroupMemberActionButton(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .dropShadow(
                        shape = CircleShape,
                        shadow = Shadow(
                            radius = 16.dp,
                            color = Color.Black.copy(alpha = 0.2f),
                        )
                    ),
                startIcon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                startIconSize = 14.dp,
                endIcon = null,
                text = "",
                showText = false,
                backgroundColor = MaterialTheme.colors.background,
                contentPadding = PaddingValues(8.dp),
                onClick = {
                    onEvent(MapHomeScreenUiEvent.OpenPersonalLocationHistory)
                }
            )
        }

        // sharing time info
        if (readyState?.selectedSharingContract != null && !isMemberOrPlaceFocused) {
            GroupMemberActionButton(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .dropShadow(
                        shape = CircleShape,
                        shadow = Shadow(
                            radius = 16.dp,
                            color = Color.Black.copy(alpha = 0.2f),
                        )
                    ),
                startIcon = CoreAppIcons.Timer.asPainterResource(),
                isLoading = false,
                text = readyState?.sharingEndTime
                    ?: stringResource(MR.strings.groups_sharing_duration_type_infinite),
                backgroundColor = MaterialTheme.colors.background,
                onClick = {
                    onEvent(MapHomeScreenUiEvent.OnSharingStartClick)
                }
            )
        }

        // sharing start
        if (readyState != null && readyState.selectedSharingContract == null && !isMemberOrPlaceFocused) {
            GroupMemberActionButton(
                modifier = Modifier
                    .padding(bottom = 6.dp),
                startIcon = CoreAppIcons.ShareLocation.asPainterResource(),
                isLoading = false,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                text = if (readyState.isPersonalSpace) {
                    stringResource(MR.strings.location_sharing_button_start_personal)
                } else {
                    stringResource(MR.strings.location_sharing_button_start_groups)
                },
                onClick = {
                    onEvent(MapHomeScreenUiEvent.OnSharingStartClick)
                }
            )
        }
    }
}

@Composable
fun CloseFocusButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
//    FilledButton(
//        modifier = modifier.size(32.dp),
//        onClick = onClick,
//        icon = CoreAppIcons.Close.asPainterResource(),
//        style = ButtonStyle.ICON,
//        content = null
//    )
    Icon(
        modifier = modifier
            .size(32.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.redMain)
            .padding(4.dp),
        painter = CoreAppIcons.Close.asPainterResource(),
        tint = AppColors.lightBackground,
        contentDescription = null
    )
}

@Composable
private fun MapGroupOrSpacePickerTabs(
    modifier: Modifier = Modifier,
    isPersonalSpace: Boolean,
    groupImage: ImageModel?,
    spaceImage: ImageModel,
    onGroupItemClick: () -> Unit,
    onGroupPickerClick: () -> Unit,
    onSpaceItemClick: () -> Unit
) {
    val position by animateFloatAsState(
        if (isPersonalSpace) -1f else 1f
    )

    val indication = LocalIndication.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colors.background)
                .background(
                    AppColors.accent.copy(alpha = animateFloatAsState(if (isPersonalSpace) 0f else 0.1f).value)
                )
        )

        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = BiasAlignment(position, 0f)
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = animateDpAsState(
                            if (isPersonalSpace) 92.dp else 132.dp
                        ).value,
                        height = 56.dp
                    )
                    .fillMaxWidth(0.5f)
                    .indication(
                        interactionSource = interactionSource,
                        indication = indication,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = indication,
                        onClick = {
                            if (isPersonalSpace) {
                                onSpaceItemClick()
                            } else {
                                onGroupItemClick()
                            }
                        }
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(100)
                    )
                    .clip(RoundedCornerShape(100))
                    .background(MaterialTheme.colors.background)
                    .background(
                        AppColors.accent.copy(alpha = animateFloatAsState(if (isPersonalSpace) 0.1f else 0f).value)
                    )
            )
        }

        Row(
            modifier = Modifier
                .height(height = 56.dp)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(100)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MapPersonalSpacePickerTab(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .clickable(
                        interactionSource = if (isPersonalSpace) interactionSource else remember { MutableInteractionSource() },
                        indication = if (isPersonalSpace) indication else LocalIndication.current,
                        onClick = onSpaceItemClick
                    ),
                isSelected = isPersonalSpace,
                image = spaceImage,
            )

            MapGroupPickerTab(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = if (!isPersonalSpace) interactionSource else remember { MutableInteractionSource() },
                        indication = if (!isPersonalSpace) indication else LocalIndication.current,
                        onClick = onGroupItemClick
                    ),
                image = groupImage,
                isSelected = !isPersonalSpace,
                onExpandClick = onGroupPickerClick
            )
        }
    }
}

@Composable
private fun MapGroupPickerTab(
    modifier: Modifier = Modifier,
    image: ImageModel?,
    isSelected: Boolean,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Spacer(modifier = Modifier.size(animateDpAsState(if (isSelected) 14.dp else 16.dp).value))

        if (image != null) {
            ImageModelImage(
                imageModel = image,
                useBackgroundForEmoji = false,
                clipShape = CircleShape,
                modifier = Modifier
                    .size(animateDpAsState(if (isSelected) 56.dp else 40.dp).value)
            )
        } else {
            Icon(
                modifier = Modifier
                    .size(animateDpAsState(if (isSelected) 56.dp else 40.dp).value)
                    .padding(animateDpAsState(if (isSelected) 10.dp else 8.dp).value),
                painter = CoreAppIcons.Communication.asPainterResource(),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.size(animateDpAsState(if (isSelected) 10.dp else 4.dp).value))

        Box(
            modifier = Modifier
                .size(
                    height = animateDpAsState(if (isSelected) 36.dp else 26.dp).value,
                    width = 0.5.dp
                )
                .background(
                    color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onExpandClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.size(animateDpAsState(if (isSelected) 8.dp else 4.dp).value))
            Icon(
                painter = CoreAppIcons.ExpandMore.asPainterResource(),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
                    .padding(animateDpAsState(if (isSelected) 0.dp else 2.dp).value),
            )
            Spacer(modifier = Modifier.size(animateDpAsState(if (isSelected) 8.dp else 6.dp).value))
        }
    }
}

@Composable
private fun MapPersonalSpacePickerTab(
    modifier: Modifier = Modifier,
    image: ImageModel,
    isSelected: Boolean,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        ImageModelImage(
            imageModel = image,
            useBackgroundForEmoji = false,
            clipShape = CircleShape,
            modifier = Modifier
                .size(animateDpAsState(if (isSelected) 56.dp else 40.dp).value)
        )
    }
}