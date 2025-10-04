package app.paralino.core.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.history.LocationHistoryDataViewType
import app.paralino.core.presentation.history.LocationHistoryPathViewMode
import app.paralino.core.presentation.history.LocationHistoryScreenFocusType
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.theme.AppColors
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
            DefaultToolbar(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .shadow(elevation = 8.dp, RoundedCornerShape(100))
                    .clip(RoundedCornerShape(100)),
                titleComposable = {
                    Column(
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = stringResource(MR.strings.groups_sharing_setting_timeline_title),
                            style = MaterialTheme.typography.subtitle1.copy(
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colors.onBackground
                        )
                        Row(
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (uiState.groupImage != null) {
                                ImageModelImage(
                                    imageModel = uiState.groupImage!!,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                                            shape = RoundedCornerShape(4.dp)
                                        ),
                                    clipShape = RoundedCornerShape(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = uiState.groupName ?: "",
                                style = MaterialTheme.typography.caption.copy(lineHeight = 14.sp),
                                color = MaterialTheme.colors.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    ToolbarButtons.Back(shape = CircleShape) {
                        onEvent(LocationHistoryUiEvent.OnCloseScreen)
                    }
                },
                actions = {
                    if (uiState.isCurrentUser) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clickable { onEvent(LocationHistoryUiEvent.OpenLocationSettings) }
                                .background(
                                    color = MaterialTheme.colors.surface,
                                    shape = CircleShape
                                )
                                .padding(10.dp),
                            painter = CoreAppIcons.Edit.asPainterResource(),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
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

        Column(
            modifier = Modifier.padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.Bottom
        ) {
            AnimatedVisibility(
                modifier = Modifier.align(alignment = Alignment.End),
                visible = uiState.isFilterOpened
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    HistoryMapOptionChip(
                        modifier = Modifier.padding(top = 4.dp),
                        selected = uiState.viewMode == LocationHistoryPathViewMode.OVERVIEW,
                        name = stringResource(MR.strings.history_map_options_location_clustering),
                        icon = CoreAppIcons.TargetCircles.asPainterResource(),
                        onClick = { onEvent(LocationHistoryUiEvent.ToggleClusterLocations) }
                    )

                    AnimatedVisibility(uiState.showPins) {
                        HistoryMapOptionChip(
                            modifier = Modifier.padding(top = 4.dp),
                            selected = uiState.clusterPins,
                            name = stringResource(MR.strings.history_map_options_pin_clustering),
                            icon = CoreAppIcons.CircleCluster.asPainterResource(),
                            onClick = { onEvent(LocationHistoryUiEvent.ToggleClusterPins) }
                        )
                    }

                    HistoryMapOptionChip(
                        modifier = Modifier.padding(top = 4.dp),
                        selected = uiState.showPins,
                        name = stringResource(MR.strings.history_map_options_show_pins),
                        icon = CoreAppIcons.LocationOn.asPainterResource(),
                        onClick = { onEvent(LocationHistoryUiEvent.ToggleShowPins) }
                    )

                    HistoryMapOptionChip(
                        modifier = Modifier.padding(top = 4.dp),
                        selected = uiState.showPolyline,
                        name = stringResource(MR.strings.history_map_options_show_path),
                        icon = CoreAppIcons.PolylineWithDots.asPainterResource(),
                        onClick = { onEvent(LocationHistoryUiEvent.ToggleShowPolyline) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .fillMaxWidth()
                    .padding(end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                FocusTypeIcon(
                    isVisible = uiState.focusType != LocationHistoryScreenFocusType.OVERVIEW && uiState.dataViewType == LocationHistoryDataViewType.LocationPoint,
                    type = LocationHistoryScreenFocusType.OVERVIEW,
                    icon = CoreAppIcons.ZoomOutFocusMap.asPainterResource(),
                    onEvent = onEvent
                )

//                FocusTypeIcon(
//                    isVisible = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint && uiState.focusType == LocationHistoryScreenFocusType.NONE,
//                    type = LocationHistoryScreenFocusType.MEMBER_DEFAULT,
//                    icon = CoreAppIcons.ZoomFocusSmall.asPainterResource(),
//                    onEvent = onEvent
//                )

                FocusTypeIcon(
                    isVisible = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint && (uiState.focusType == LocationHistoryScreenFocusType.MEMBER_DEFAULT || uiState.focusType == LocationHistoryScreenFocusType.OVERVIEW),
                    type = LocationHistoryScreenFocusType.MEMBER_FOCUSED,
                    icon = CoreAppIcons.ZoomFocusFull.asPainterResource(),
                    onEvent = onEvent
                )

                Icon(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .clickable(onClick = { onEvent(LocationHistoryUiEvent.ToggleMapFilter) })
                        .shadow(elevation = animateDpAsState(if (uiState.isFilterOpened) 4.dp else 0.dp).value, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.background)
                        .padding(4.dp)
                        .rotate(animateFloatAsState(if (uiState.isFilterOpened) 90f else -90f).value),
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null
                )
            }

            Column(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        cardHeight(with(density) { coordinates.size.height.toDp() })
                    }
            ) {
                AnimatedVisibility(
                    visible = uiState.daySelectorState.displayedDays.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LocationHistoryDetailCard(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp),
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }

                AnimatedVisibility(
                    visible = uiState.daySelectorState.displayedDays.isNotEmpty() && uiState.focusType != LocationHistoryScreenFocusType.NONE,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LocationHistoryDateSelector(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 8.dp),
                        elevation = 4.dp,
                        state = uiState.daySelectorState,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.FocusTypeIcon(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    type: LocationHistoryScreenFocusType,
    icon: Painter,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible
    ) {
        Icon(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(32.dp)
                .clickable(onClick = {
                    onEvent(LocationHistoryUiEvent.OnSelectFocusType(type))
                })
//                .shadow(elevation = 4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colors.background)
                .padding(4.dp),
            painter = icon,
            contentDescription = null
        )
    }
}

@Composable
private fun HistoryMapOptionChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    name: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onBackground
    )
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colors.primary
        else MaterialTheme.colors.surface
    )

    DataInfoChip(
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(elevation = 4.dp, RoundedCornerShape(50)),
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(50),
        imageOrIcon = {
            Icon(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(16.dp),
                painter = icon,
                contentDescription = null,
                tint = contentColor.copy(
                    alpha = if (selected) 1f else 0.5f
                )
            )
            Spacer(Modifier.size(4.dp))
        },
        text = {
            Text(
                text = name.annotatedBoldCompose(textColor = contentColor),
                style = MaterialTheme.typography.body2,
                color = contentColor
            )
        }
    )
}