package app.paralino.core.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.components.GroupMember
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
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {

        val readyState = rememberUpdatedState(state.asReady()).value

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // group picker button
            AnimatedVisibility(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                visible = state is MapHomeScreenState.Ready,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                if (readyState != null) {
                    MapGroupPicker(
                        modifier = Modifier,
                        groupImage = readyState.groupImage,
                        onExpandClick = { onEvent(MapHomeScreenUiEvent.OnGroupPickerClick) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    ImageModelImage(
                        imageModel = state.currentUserImage,
                        useBackgroundForEmoji = false,
                        clipShape = CircleShape,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colors.surface,
                                shape = CircleShape
                            )
                    )
                }
            )
        }

        // map mode
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

        Spacer(modifier = Modifier.weight(1f))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = sheetPeekHeight)
            ) {
                // sharing time info
                AnimatedVisibility(
                    visible = readyState?.sharingStatus != null && !isMemberOrPlaceFocused,
                    modifier = Modifier
                        .padding(start = 6.dp, bottom = 32.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onEvent(MapHomeScreenUiEvent.OnGroupDetailsClick) }
                            .background(
                                MaterialTheme.colors.surface,
                                MaterialTheme.shapes.medium
                            )
                            .clip(RoundedCornerShape(percent = 100))
                            .padding(vertical = 4.dp)
                            .padding(start = 4.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = CoreAppIcons.Timer.asPainterResource(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = readyState?.sharingEndTime
                                ?: stringResource(MR.strings.groups_sharing_duration_type_infinite),
                            style = MaterialTheme.typography.caption.copy(fontSize = 10.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

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
                            modifier = Modifier.padding(bottom = 32.dp)
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
                            modifier = Modifier.padding(bottom = 32.dp)
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