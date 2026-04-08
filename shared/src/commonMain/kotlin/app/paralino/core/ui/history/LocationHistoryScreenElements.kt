package app.paralino.core.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.history.LocationHistoryDataViewType
import app.paralino.core.presentation.history.LocationHistoryScreenFocusType
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun LocationHistoryMapCardUi(
    modifier: Modifier = Modifier,
    uiState: LocationHistoryScreenUiState,
    cardHeight: (Dp) -> Unit,
    onEvent: (LocationHistoryUiEvent) -> Unit,
){
    val density = LocalDensity.current
    Column(
        modifier = modifier.padding(
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
                    selected = uiState.clusteringPointsMode.isClusteringOn,
                    name = stringResource(MR.strings.history_map_options_location_clustering),
                    icon = CoreAppIcons.TargetCircles.asPainterResource(),
                    onClick = { onEvent(LocationHistoryUiEvent.ToggleClusterLocations) }
                )

                AnimatedVisibility(uiState.showPins) {
                    HistoryMapOptionChip(
                        modifier = Modifier.padding(top = 4.dp),
                        selected = uiState.clusteringPinsMode.isClusteringOn,
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
                .padding(end = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            MapCardSmallActionIcon(
                modifier = Modifier.padding(end = 8.dp),
                isVisible = uiState.focusType != LocationHistoryScreenFocusType.OVERVIEW && uiState.dataViewType == LocationHistoryDataViewType.LocationPoint,
                icon = CoreAppIcons.ZoomOutFocusMap.asPainterResource(),
                onClick = {
                    onEvent(LocationHistoryUiEvent.OnSelectFocusType(LocationHistoryScreenFocusType.OVERVIEW))
                }
            )

//                FocusTypeIcon(
//                    isVisible = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint && uiState.focusType == LocationHistoryScreenFocusType.NONE,
//                    type = LocationHistoryScreenFocusType.MEMBER_DEFAULT,
//                    icon = CoreAppIcons.ZoomFocusSmall.asPainterResource(),
//                    onEvent = onEvent
//                )

            MapCardSmallActionIcon(
                modifier = Modifier.padding(end = 8.dp),
                isVisible = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint && (uiState.focusType == LocationHistoryScreenFocusType.MEMBER_DEFAULT || uiState.focusType == LocationHistoryScreenFocusType.OVERVIEW),
                icon = CoreAppIcons.ZoomFocusFull.asPainterResource(),
                onClick = {
                    onEvent(LocationHistoryUiEvent.OnSelectFocusType(LocationHistoryScreenFocusType.MEMBER_FOCUSED))
                }
            )

            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clickable(onClick = { onEvent(LocationHistoryUiEvent.ToggleMapFilter) })
                    .dropShadow(
                        shape = RoundedCornerShape(8.dp),
                        shadow = Shadow(
                            radius = animateDpAsState(if (uiState.isFilterOpened) 16.dp else 12.dp).value,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                        )
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isFilterOpened,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(90f),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        contentDescription = null
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = !uiState.isFilterOpened,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp),
                        painter = CoreAppIcons.FilterTune.asPainterResource(),
                        contentDescription = null
                    )
                }
            }

            Icon(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(32.dp)
                    .clickable(onClick = {
                        onEvent(LocationHistoryUiEvent.OnCloseScreen)
                    })
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.redMain)
                    .padding(4.dp),
                painter = CoreAppIcons.Close.asPainterResource(),
                tint = AppColors.lightBackground,
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
                    elevation = 12.dp,
                    state = uiState.daySelectorState,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
fun RowScope.MapCardSmallActionIcon(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    icon: Painter,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible
    ) {
        Icon(
            modifier = modifier
                .size(32.dp)
                .clickable(onClick = onClick)
                .dropShadow(
                    shape = RoundedCornerShape(8.dp),
                    shadow = Shadow(
                        radius = 12.dp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                    )
                )
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
        },
        endSection = null
    )
}