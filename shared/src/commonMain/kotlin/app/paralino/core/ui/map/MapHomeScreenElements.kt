package app.paralino.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.asReady
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
    topPadding: Dp,
    sheetPeekHeight: Dp,
    cardHeight: (Dp) -> Unit,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {

        val readyState = rememberUpdatedState(state.asReady()).value
        val density = LocalDensity.current

        val isMemberOrPlaceFocused =
            readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topPadding)
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
                        tint = MaterialTheme.colors.onSurface,
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
                    .padding(top = 16.dp, end = 8.dp, start = 8.dp)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = sheetPeekHeight),
                horizontalArrangement = Arrangement.End
            ) {
                // sharing time info
                AnimatedVisibility(
                    visible = readyState?.sharingStatus != null && !isMemberOrPlaceFocused,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 6.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GroupMemberActionButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        startIcon = CoreAppIcons.Timer.asPainterResource(),
                        isLoading = false,
                        text = readyState?.sharingEndTime
                            ?: stringResource(MR.strings.groups_sharing_duration_type_infinite),
                        onClick = {
                            onEvent(MapHomeScreenUiEvent.OnSharingStartClick)
                        }
                    )
                }

                // sharing start
                if (readyState != null && readyState.sharingStatus == null && !isMemberOrPlaceFocused) {
                    FilledButton(
                        modifier = Modifier.padding(bottom = 12.dp, end = 8.dp),
                        style = ButtonStyle.SMALL(
                            iconSize = 24.dp
                        ),
                        icon = CoreAppIcons.ShareLocation.asPainterResource(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        onClick = {
                            onEvent(MapHomeScreenUiEvent.OnSharingStartClick)
                        }
                    ) {
                        Text(text = stringResource(MR.strings.location_sharing_button_start_groups))
                    }
                }
            }

            Column(
                modifier = Modifier
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
                                modifier = Modifier.padding(top = 20.dp),
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            GroupMember(
                                modifier = Modifier.padding(top = 20.dp),
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
    }
}

@Composable
fun CloseFocusButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledButton(
        modifier = modifier.size(32.dp),
        onClick = onClick,
        icon = CoreAppIcons.Close.asPainterResource(),
        style = ButtonStyle.ICON,
        content = null
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
                    animateColorAsState(
                        if (!isPersonalSpace) AppColors.accent.copy(alpha = 0.1f) else MaterialTheme.colors.background
                    ).value
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
                        animateColorAsState(
                            if (isPersonalSpace) AppColors.accent.copy(alpha = 0.1f) else MaterialTheme.colors.background
                        ).value
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
                    .width(
                        animateDpAsState(
                            if (isPersonalSpace) 80.dp else 80.dp
                        ).value
                    )
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
                    .width(
                        animateDpAsState(
                            if (isPersonalSpace) 100.dp else 120.dp
                        ).value
                    )
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
        Spacer(modifier = Modifier.width(14.dp))

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
                    .padding(animateDpAsState(if (isSelected) 16.dp else 10.dp).value),
                painter = CoreAppIcons.Group.asPainterResource(),
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.size(animateDpAsState(if (isSelected) 10.dp else 6.dp).value))

        Box(
            modifier = Modifier
                .size(height = 36.dp, width = 0.5.dp)
                .scale(animateFloatAsState(if (isSelected) 1f else 0.8f).value)
                .background(
                    color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                )
        )
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(animateDpAsState(if (isSelected) 40.dp else 36.dp).value)
                .clickable(onClick = onExpandClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = CoreAppIcons.ExpandMore.asPainterResource(),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(animateDpAsState(if (isSelected) 0.dp else 4.dp).value))
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