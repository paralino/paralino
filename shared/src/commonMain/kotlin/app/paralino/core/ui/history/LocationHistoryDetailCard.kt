package app.paralino.core.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.history.LocationHistoryDataViewType
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.EmbeddedSlider
import app.paralino.core.ui.components.EmbeddedSliderSize
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.GroupStatusTag
import app.paralino.core.ui.groups.GroupStatusTagType
import app.paralino.core.ui.groups.components.GroupMemberActionButton
import app.paralino.core.ui.groups.components.NetworkTypeIcon
import app.paralino.core.ui.groups.components.actionIcon
import app.paralino.core.ui.groups.components.actionText
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional
import app.paralino.core.utils.extensions.isLocationInaccurate
import app.paralino.core.utils.extensions.pulsate
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

private val cornerShape = 20.dp
private val innerCornerShape = 15.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationHistoryDetailCard(
    modifier: Modifier = Modifier,
    isCompactUiMode: Boolean,
    uiState: LocationHistoryScreenUiState,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {

    val handleEvent = onEvent

    val isCollapsed = uiState.isCardCollapsed

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        handleEvent(LocationHistoryUiEvent.ToggleExpandedCollapseFocusType)
                    }
                )
                .dropShadow(
                    shape = RoundedCornerShape(cornerShape),
                    shadow = Shadow(
                        radius = 8.dp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                    )
                )
                .clip(RoundedCornerShape(cornerShape))
                .background(MaterialTheme.colors.background)
                .conditional(isCompactUiMode) {
                    verticalScroll(rememberScrollState())
                }
        ) {
            // devices and view switcher
            AnimatedVisibility(visible = !isCollapsed) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scrollState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()
                    LazyRow(
                        modifier = Modifier
                            .weight(1f),
                        state = scrollState,
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp)
                    ) {
                        if (uiState.activeDevices.size > 1) {
                            this.item(key = "title") {
                                Text(
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(end = 8.dp),
                                    text = stringResource(MR.strings.group_member_devices_section_title),
                                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        this.itemsIndexed(
                            uiState.activeDevices,
                            key = { index, device -> device.locationId }) { index, device ->
                            UserDeviceChip(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(end = 6.dp),
                                name = device.deviceName,
                                isSelected = device.isSelected,
                                isCurrent = device.isCurrent,
                                hasLocations = device.hasLocations,
                                showIcon = uiState.activeDevices.size == 1,
                                onClick = {
                                    coroutineScope.launch {
                                        scrollState.animateScrollToItem(index = index)
                                    }
                                    handleEvent(LocationHistoryUiEvent.SelectDevice(device = device))
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(width = 90.dp, height = 36.dp)
                            .clip(RoundedCornerShape(innerCornerShape))
                            .background(MaterialTheme.colors.surface)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HistoryDataViewTypeTab(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            selected = uiState.dataViewType == LocationHistoryDataViewType.Stats,
                            icon = CoreAppIcons.BarChartStats.asPainterResource(),
                            onClick = {
                                handleEvent(
                                    LocationHistoryUiEvent.ChangeMapDataViewType(
                                        LocationHistoryDataViewType.Stats
                                    )
                                )
                            }
                        )

                        HistoryDataViewTypeTab(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            selected = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint,
                            icon = CoreAppIcons.RouteLineWithDotsOnEnds.asPainterResource(),
                            onClick = {
                                handleEvent(
                                    LocationHistoryUiEvent.ChangeMapDataViewType(
                                        LocationHistoryDataViewType.LocationPoint
                                    )
                                )
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(
                        bottom = 6.dp,
                        top = animateDpAsState(if (isCollapsed) 6.dp else 0.dp).value
                    )
                    .padding(horizontal = 6.dp)
                    .conditional(uiState.isLoadingLocations) {
                        pulsate(pulseAmount = 0.98f)
                    }
                    .alpha(animateFloatAsState(if (uiState.isLoadingLocations) 0.5f else 1f).value)
                    .clip(
                        RoundedCornerShape(
                            topStart = innerCornerShape,
                            topEnd = if (uiState.deleteAction != null) 16.dp else innerCornerShape,
                            bottomStart = innerCornerShape,
                            bottomEnd = if (uiState.navigateAction != null) 16.dp else innerCornerShape
                        )
                    )
                    .background(MaterialTheme.colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // image
                        Box(
                            modifier = Modifier
                                .width(IntrinsicSize.Min)
                                .align(Alignment.Top)
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(50.dp)
                                    .heightIn(min = 50.dp)
                            ) {
                                if (uiState.userImage != null) {
                                    ImageModelImage(
                                        imageModel = uiState.userImage!!,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .border(
                                                width = if (uiState.isCurrentUser) {
                                                    2.dp
                                                } else {
                                                    0.5.dp
                                                },
                                                color = if (uiState.isCurrentUser) {
                                                    MaterialTheme.colors.primary
                                                } else {
                                                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )
                                }
                            }
                        }

                        // center content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 50.dp)
                                .padding(start = 12.dp, end = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.userName ?: "",
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                            )

                            when (uiState.dataViewType) {
                                LocationHistoryDataViewType.LocationPoint -> {
                                    uiState.selectedPoint?.let { point ->
                                        LocationTime(time = point.locationTime)

                                        SpeedAndElevationData(point = point)

                                        AnimatedVisibility(
                                            visible = point is LocationHistoryPointState.Point && point.point.loc?.acc.isLocationInaccurate()
                                        ) {
                                            GroupStatusTag(status = GroupStatusTagType.INACTIVE_NOT_ACCURATE)
                                        }
                                    }
                                }

                                LocationHistoryDataViewType.Stats -> {
                                    LocationTime(time = uiState.stats?.formattedTimeRange)
                                }
                            }
                        }
                    }

                    if (uiState.deleteAction != null && !isCollapsed) {
                        GroupMemberActionButton(
                            modifier = Modifier
                                .padding(end = 2.dp, top = 2.dp)
                                .align(Alignment.Top),
                            startIcon = MemberActionType.DELETE_LOCATION.actionIcon,
                            endIcon = null,
                            isLoading = uiState.deleteAction!!.isLoading,
                            contentPadding = PaddingValues(6.dp),
                            text = stringResource(MemberActionType.DELETE_LOCATION.actionText),
                            showText = false,
                            backgroundColor = AppColors.errorBackground,
                            contentColor = AppColors.redMain,
                            onClick = {
                                handleEvent(LocationHistoryUiEvent.OnDeleteSelectedLocationRequest)
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint) {
                    Column {
                        AnimatedVisibility(uiState.selectedPoint?.statusTime != null && !isCollapsed) {
                            FlowRow(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(MR.strings.group_members_last_status_update).annotatedBoldCompose(),
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                )
                                Text(
                                    text = uiState.selectedPoint?.statusTime?.let { " $it" } ?: "",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
//                                .padding(start = 2.dp)
//                                .padding(bottom = 10.dp)
//                                .requiredHeight(30.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (uiState.selectedPoint) {
                                is LocationHistoryPointState.Cluster -> {
                                    AnimatedVisibility(!isCollapsed) {
                                        GroupMemberActionButton(
                                            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                                            startIcon = CoreAppIcons.TargetCircles.asPainterResource(),
                                            endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                                            text = stringResource(
                                                MR.strings.history_entry_clustered_count,
                                                "${uiState.selectedPoint.points.size}"
                                            ),
                                            backgroundColor = if (isSystemInDarkTheme()) {
                                                AppColors.accentLight.copy(alpha = 0.2f)
                                            } else {
                                                AppColors.accent.copy(alpha = 0.2f)
                                            },
                                            onClick = {
                                                handleEvent(LocationHistoryUiEvent.ExpandCluster(uiState.selectedPoint))
                                            }
                                        )
                                    }
                                }

                                is LocationHistoryPointState.Point -> {
                                    Row(
                                        modifier = Modifier
                                            .padding(start = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BatteryAndNetworkData(point = uiState.selectedPoint)
                                    }
                                }

                                null -> {}
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp, bottom = 2.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                if (uiState.rawDataAction != null && !isCollapsed) {
                                    GroupMemberActionButton(
                                        modifier = Modifier,
                                        startIcon = CoreAppIcons.Security.asPainterResource(),
                                        endIcon = null,
                                        text = "",
                                        showText = false,
                                        contentPadding = PaddingValues(6.dp),
                                        backgroundColor = if (isSystemInDarkTheme()) {
                                            AppColors.accentLight.copy(alpha = 0.2f)
                                        } else {
                                            AppColors.accent.copy(alpha = 0.2f)
                                        },
                                        onClick = {
                                            handleEvent(
                                                LocationHistoryUiEvent.OnRawDataPointClicked(action = uiState.rawDataAction)
                                            )
                                        }
                                    )
                                }

                                if (uiState.navigateAction != null) {
                                    GroupMemberActionButton(
                                        modifier = Modifier,
                                        startIcon = CoreAppIcons.Directions.asPainterResource(),
                                        endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                                        text = stringResource(MR.strings.group_members_item_directions_button),
                                        backgroundColor = if (isSystemInDarkTheme()) {
                                            AppColors.accentLight.copy(alpha = 0.2f)
                                        } else {
                                            AppColors.accent.copy(alpha = 0.2f)
                                        },
                                        onClick = {
                                            handleEvent(
                                                LocationHistoryUiEvent.OnDirectionsRequest(action = uiState.navigateAction!!)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.selectedLocations.isEmpty() && !isCollapsed) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .padding(bottom = 20.dp),
                            text = stringResource(MR.strings.history_map_no_locations_placeholder),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.dataViewType == LocationHistoryDataViewType.Stats && !isCollapsed) {
                uiState.stats?.let {
                    LocationHistoryStatsTable(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .padding(horizontal = 6.dp)
                            .conditional(uiState.isLoadingLocations) {
                                pulsate(pulseAmount = 0.98f)
                            }
                            .alpha(animateFloatAsState(if (uiState.isLoadingLocations) 0.5f else 1f).value),
                        isCompactMode = isCompactUiMode,
                        stats = it
                    )
                }
            }

            AnimatedVisibility(visible = uiState.dataViewType == LocationHistoryDataViewType.LocationPoint && !isCollapsed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.selectedLocations.size > 1) {

                        AnimatedVisibility(
                            visible = uiState.locationsToDeleteSize > 0
                        ) {
                            Text(
                                modifier = Modifier,
                                text = stringResource(
                                    MR.strings.history_entry_range_delete_notice_title,
                                    uiState.locationsToDeleteSize
                                ),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        val sliderSize = remember {
                            val factor = maxOf(
                                0,
                                (uiState.selectedLocations.size.toString().length - 1) / 3
                            ) * 15
                            EmbeddedSliderSize(minStepWidth = (80 + factor).dp, minHeight = 40.dp)
                        }
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(sliderSize.minHeight)
                                    .clickable(enabled = uiState.hasPreviousLocation) {
                                        onEvent(LocationHistoryUiEvent.NavigateHistoryPrevious)
                                    }
                                    .clip(RoundedCornerShape(innerCornerShape))
                                    .background(
                                        color = if (uiState.hasPreviousLocation) {
                                            if (isSystemInDarkTheme()) {
                                                AppColors.accentLight.copy(alpha = 0.2f)
                                            } else {
                                                AppColors.accent.copy(alpha = 0.2f)
                                            }
                                        } else {
                                            MaterialTheme.colors.surface.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(innerCornerShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(22.dp),
                                    painter = CoreAppIcons.ChevronLeft.asPainterResource(),
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colors.onBackground.copy(alpha = if (uiState.hasPreviousLocation) 1f else 0.3f)
                                )
                            }

                            EmbeddedSlider(
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(animateFloatAsState(if (uiState.isLoadingLocations) 0.5f else 1f).value)
                                    .height(sliderSize.minHeight),
                                infoContent = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colors.surface.copy(alpha = 0.9f))
                                            .animateContentSize()
                                            .padding(horizontal = 6.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            modifier = Modifier,
                                            text = uiState.locationIndex.plus(1).toString(),
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            lineHeight = 10.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colors.onSurface
                                        )

                                        Text(
                                            modifier = Modifier,
                                            text = "\\ ${uiState.selectedLocations.size}",
                                            style = MaterialTheme.typography.caption,
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                    }
                                },
                                swipeEnabled = !uiState.isLoadingLocations,
                                size = sliderSize,
                                progress = uiState.locationIndex,
                                shape = RoundedCornerShape(innerCornerShape),
                                steps = uiState.selectedLocations.size,
                                onProgressChange = {
                                    handleEvent(LocationHistoryUiEvent.SetHistoryProgress(it))
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .size(sliderSize.minHeight)
                                    .clickable(enabled = uiState.hasNextLocation) {
                                        onEvent(LocationHistoryUiEvent.NavigateHistoryNext)
                                    }
                                    .clip(RoundedCornerShape(innerCornerShape))
                                    .background(
                                        color = if (uiState.hasNextLocation) {
                                            if (isSystemInDarkTheme()) {
                                                AppColors.accentLight.copy(alpha = 0.2f)
                                            } else {
                                                AppColors.accent.copy(alpha = 0.2f)
                                            }
                                        } else {
                                            MaterialTheme.colors.surface.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(innerCornerShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(22.dp),
                                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colors.onBackground.copy(alpha = if (uiState.hasNextLocation) 1f else 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.daySelectorState.displayedDays.isNotEmpty() && !isCollapsed && isCompactUiMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LocationHistoryDateSelector(
                    modifier = Modifier
                        .padding(
                            top = if (uiState.dataViewType == LocationHistoryDataViewType.LocationPoint) 6.dp else 0.dp,
                            bottom = 6.dp
                        )
                        .padding(horizontal = 6.dp),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 0.dp,
                    cornerRadiusDp = innerCornerShape,
                    state = uiState.daySelectorState,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LocationTime(
    modifier: Modifier = Modifier,
    time: String?
) {
    if (time != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = CoreAppIcons.AccessTime.asPainterResource(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(16.dp)
            )
            Text(
                text = time.annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun UserDeviceChip(
    modifier: Modifier = Modifier,
    name: String,
    isSelected: Boolean,
    isCurrent: Boolean,
    hasLocations: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onSurface.copy(
            if (hasLocations) 1f else 0.4f
        )
    )
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colors.primary
        else MaterialTheme.colors.surface.copy(
            if (hasLocations) 1f else 0.6f
        )
    )

    DataInfoChip(
        modifier = modifier
            .border(
                width = if (isCurrent) 0.5.dp else 0.dp,
                color = MaterialTheme.colors.primary.copy(
                    if (isCurrent) 0.8f else 0f
                ),
                shape = RoundedCornerShape(100)
            ),
        shape = RoundedCornerShape(100),
        backgroundColor = backgroundColor,
        imageOrIcon = if (showIcon) {
            {
                Icon(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                    contentDescription = null,
                    tint = contentColor
                )
            }
        } else if (hasLocations) {
            {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(4.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary,
                            shape = CircleShape
                        )
                )
            }
        } else {
            null
        },
        text = {
            Text(
                text = name,
                color = contentColor,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier
            )
        },
        endSection = null,
        onClick = onClick
    )
}

@Composable
private fun ColumnScope.SpeedAndElevationData(
    point: LocationHistoryPointState
) {
    var speed: String by remember {
        mutableStateOf(point.formattedSpeed ?: "")
    }

    var elevation: String by remember {
        mutableStateOf(point.formattedElevation ?: "")
    }

    LaunchedEffect(point.formattedSpeed, point.formattedElevation) {
        if (!point.formattedSpeed.isNullOrBlank()) {
            speed = point.formattedSpeed ?: ""
        }

        if (!point.formattedElevation.isNullOrBlank()) {
            elevation = point.formattedElevation ?: ""
        }
    }

    AnimatedVisibility(point.formattedSpeed != null || point.formattedElevation != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(point.formattedSpeed != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = CoreAppIcons.Speed.asPainterResource(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = speed,
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }

            AnimatedVisibility(point.formattedElevation != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = CoreAppIcons.Elevation.asPainterResource(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = elevation,
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.BatteryAndNetworkData(
    point: LocationHistoryPointState.Point
) {
    if (point.point.status.bat != null) {
        Icon(
            painter = if (point.point.status.isCharging) {
                CoreAppIcons.BatteryChargingFull.asPainterResource()
            } else {
                CoreAppIcons.BatteryFull.asPainterResource()
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 2.dp)
                .size(18.dp)
        )
        Text(
            text = "${point.point.status.bat}%",
            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }

    if (point.networkType != null) {
        NetworkTypeIcon(
            point.networkType!!,
            point.signalStrength
        )
        Text(
            text = point.networkType!!.networkName,
            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun HistoryDataViewTypeTab(
    modifier: Modifier = Modifier,
    selected: Boolean,
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

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(18.dp),
            painter = icon,
            contentDescription = null,
            tint = contentColor.copy(
                alpha = if (selected) 1f else 0.5f
            )
        )
    }
}