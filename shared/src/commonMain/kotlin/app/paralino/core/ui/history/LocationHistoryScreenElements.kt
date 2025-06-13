package app.paralino.core.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.history.LocationHistoryMapFocusType
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.map.MapGroupPicker
import app.paralino.core.ui.places.PlaceListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun LocationHistoryScreenElements(
    modifier: Modifier = Modifier,
    uiState: LocationHistoryScreenState,
    topPadding: Dp,
    cardHeight: (Dp) -> Unit,
    onEvent: (LocationHistoryUiEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {

        val isPinFocused = uiState.mapFocusType.isLocationPoint
        val density = LocalDensity.current

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
//            AnimatedVisibility(
//                modifier = Modifier
//                    .padding(horizontal = 8.dp),
//                visible = state is MapHomeScreenState.Ready,
//                enter = fadeIn() + slideInVertically(),
//                exit = fadeOut() + slideOutVertically()
//            ) {
//                if (readyState != null) {
//                    MapGroupPicker(
//                        modifier = Modifier,
//                        groupImage = readyState.groupImage,
//                        onExpandClick = { onEvent(MapHomeScreenUiEvent.OnGroupPickerClick) }
//                    )
//                }
//            }

            Box(modifier = Modifier.padding(start = 4.dp)) {
                ToolbarButtons.Back { onEvent(LocationHistoryUiEvent.OnCloseScreen) }
            }

            Spacer(modifier = Modifier.weight(1f))

            FilledButton(
                modifier = Modifier.padding(end = 8.dp).size(42.dp),
                onClick = { onEvent(LocationHistoryUiEvent.ToggleDayRange) },
                icon = null,
                style = ButtonStyle.ICON,
                elevation = 8.dp,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface
                ),
                content = {
                    Text(
                        text = "${uiState.dateRangeDays}",
                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
                    )
                }
            )
        }

        // map mode
        Image(
            painter = if (!uiState.isSatelliteMode) {
                MR.images.image_map_type_satellite.asPainterResource()
            } else if (isSystemInDarkTheme()) {
                MR.images.image_map_type_default_dark.asPainterResource()
            } else {
                MR.images.image_map_type_default_light.asPainterResource()
            },
            contentDescription = null,
            modifier = Modifier
                .align(
                    when (uiState.deviceType) {
                        DeviceType.Android -> Alignment.End
                        DeviceType.iOS -> Alignment.Start
                    }
                )
                .padding(top = 16.dp, end = 8.dp, start = 8.dp)
                .size(40.dp)
                .clickable { onEvent(LocationHistoryUiEvent.OnMapModeSwitch) }
                .background(
                    color = AppColors.accentDark,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp)
            ) {
                this.items(uiState.devices, key = { it.locationId }) { device ->
                    UserDeviceChip(
                        modifier = Modifier.padding(end = 12.dp),
                        selected = device.isSelected,
                        name = device.deviceName,
                        onClick = {
                            onEvent(LocationHistoryUiEvent.SelectDevice(device = device))
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    cardHeight(with(density) { coordinates.size.height.toDp() })
                }
            ) {
                AnimatedVisibility(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp),
                    visible = uiState.mapFocusType is LocationHistoryMapFocusType.LocationPoint,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
//                    readyState?.focusedMember?.let { memberState ->
//                        Box(
//                            modifier = Modifier.padding(bottom = 32.dp)
//                        ) {
//                            GroupMember(
//                                modifier = Modifier.padding(top = 20.dp),
//                                state = memberState,
//                                backgroundColor = MaterialTheme.colors.background,
//                                shape = RoundedCornerShape(16.dp),
//                                collapseOnActionClick = false,
//                                forceExpandAction = true,
//                                canShowOldLocation = true,
//                                onEvent = { event ->
//                                    onEvent(MapHomeScreenUiEvent.OnMemberItemEvent(event = event))
//                                }
//                            )
//                            CloseFocusButton(
//                                modifier = Modifier
//                                    .align(Alignment.TopEnd)
//                                    .padding(end = 12.dp),
//                                onClick = {
//                                    onEvent(MapHomeScreenUiEvent.OnCloseFocusedItemClick)
//                                }
//                            )
//                        }
//                    }
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
private fun UserDeviceChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    name: String,
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

    DataInfoChip(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        backgroundColor = backgroundColor,
        imageOrIcon = {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                contentDescription = null,
                tint = contentColor
            )
        },
        hasEndArrow = false,
        text = {
            Text(
                text = name,
                color = contentColor,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        onClick = onClick
    )
}