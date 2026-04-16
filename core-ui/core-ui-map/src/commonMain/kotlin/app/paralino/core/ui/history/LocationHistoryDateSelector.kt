package app.paralino.core.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.history.LocationHistoryDateSelectionAction
import app.paralino.core.presentation.history.LocationHistoryDateSelectorState
import app.paralino.core.presentation.history.LocationHistoryDayState
import app.paralino.core.presentation.history.LocationHistorySelectedDayPosition
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.onboardingContentTransitionSpec
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun LocationHistoryDateSelector(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    elevation: Dp,
    cornerRadiusDp: Dp,
    state: LocationHistoryDateSelectorState,
    onEvent: (LocationHistoryUiEvent) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Scrollable days container
        var scrollOnInit by rememberSaveable { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .weight(1f, fill = state.isLimitedHistoryPlanMode)
                .wrapContentWidth()
                .dropShadow(
                    shape = RoundedCornerShape(cornerRadiusDp),
                    shadow = Shadow(
                        radius = elevation,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                    )
                )
                .clip(RoundedCornerShape(cornerRadiusDp))
                .background(backgroundColor)
        ) {
            val scrollState = rememberLazyListState(0)
            val scope = rememberCoroutineScope()

            LaunchedEffect(state.displayedDays, state.dateRangeDays) {
                if (state.forceFocusOnSelectedDay || scrollOnInit) {
                    scrollOnInit = false
                    val selectedItemIndex =
                        state.displayedDays.indexOfFirst { it.isSelected }.takeIf { it >= 0 }
                    if (selectedItemIndex != null) {
                        scope.launch {
                            scrollState.animateScrollToItem(
                                index = (selectedItemIndex - 1).coerceAtLeast(0)
                            )
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.wrapContentWidth(),
                contentPadding = PaddingValues(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                reverseLayout = true,
                userScrollEnabled = !state.isLimitedHistoryPlanMode,
                state = scrollState
            ) {
                // Day cells
                itemsIndexed(
                    items = state.displayedDays,
                    key = { index, item -> item.date.toString() }
                ) { index, dayState ->
                    DayCell(
                        dayState = dayState,
                        cornerRadiusDp = remember(cornerRadiusDp) { cornerRadiusDp.value.times(0.666667f).toInt().dp },
                        onDaySelected = {
                            scope.launch {
                                scrollState.animateScrollToItem(
                                    index = (index - 1).coerceAtLeast(0)
                                )
                            }
                            onEvent(LocationHistoryUiEvent.OnSelectDay(it))
                        },
                        onDayLongClick = {
                            onEvent(LocationHistoryUiEvent.OnLongSelectDay(it))
                        }
                    )
                }

                if (!state.isLimitedHistoryPlanMode) {
                    item {
                        DateSelectorActionButton(
                            action = state.startAction,
                            onClick = {
                                onEvent(LocationHistoryUiEvent.OnDateSelectorAction(state.startAction))
                            }
                        )
                    }
                }
            }

            if (state.isLimitedHistoryPlanMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onEvent(LocationHistoryUiEvent.OnDateSelectorAction(state.startAction))
                            }
                        )
                        .background(MaterialTheme.colors.background.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    DateSelectorActionButton(
                        action = state.startAction,
                        onClick = {
                            onEvent(LocationHistoryUiEvent.OnDateSelectorAction(state.startAction))
                        }
                    )
                }
            }
        }

        val dateRangeDays = state.dateRangeDays
        AnimatedVisibility(
            visible = dateRangeDays != null && dateRangeDays > 1 && !state.isLimitedHistoryPlanMode,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            // Day range button
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(width = 50.dp, height = 48.dp)
                    .clickable { onEvent(LocationHistoryUiEvent.ToggleDayRange) }
                    .dropShadow(
                        shape = RoundedCornerShape(cornerRadiusDp),
                        shadow = Shadow(
                            radius = elevation,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                        )
                    )
                    .clip(RoundedCornerShape(cornerRadiusDp))
                    .background(backgroundColor),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = state.dateRangeDays ?: 1,
                    label = "animated count",
                    transitionSpec = onboardingContentTransitionSpec()
                ) {
                    Text(
                        style = MaterialTheme.typography.caption,
                        text = "${state.dateRangeDays ?: 1}x",
                        color = if (state.isMaxRange) {
                            AppColors.redMain
                        } else {
                            MaterialTheme.colors.onSurface
                        },
                        fontSize = 16.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSelectorActionButton(
    modifier: Modifier = Modifier,
    action: LocationHistoryDateSelectionAction,
    onClick: () -> Unit
) {
    when (action) {
        LocationHistoryDateSelectionAction.LOAD_MODE -> {
            Box(
                modifier = modifier
                    .clickable(onClick = onClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = CoreAppIcons.ChevronLeft.asPainterResource(),
                    contentDescription = "Load previous dates",
                    tint = MaterialTheme.colors.primary
                )
            }
        }

        LocationHistoryDateSelectionAction.UPGRADE -> {
            SmallActionButton(
                modifier = modifier.padding(start = 6.dp, end = 12.dp),
                icon = CoreAppIcons.Add.asPainterResource(),
                text = {
                    Text(
                        modifier = Modifier.padding(end = 2.dp),
                        text = stringResource(MR.strings.history_map_date_add_more_button)
                    )
                },
                hasEndArrow = false,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                onClick = onClick
            )
        }

        LocationHistoryDateSelectionAction.ASK_FOR_UPGRADE -> {
            SmallActionButton(
                modifier = modifier.padding(start = 6.dp, end = 12.dp),
                icon = null,
                text = {
                    Text(
                        modifier = Modifier.padding(end = 2.dp),
                        text = stringResource(MR.strings.history_map_date_need_more_button)
                    )
                },
                hasEndArrow = true,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                onClick = onClick
            )
        }

        LocationHistoryDateSelectionAction.END,
        LocationHistoryDateSelectionAction.NONE -> {

        }
    }
}

@Composable
private fun DayCell(
    dayState: LocationHistoryDayState,
    cornerRadiusDp: Dp,
    onDaySelected: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
) {
    // Animate corner radius based on position
    val shape = when (dayState.position) {
        LocationHistorySelectedDayPosition.START -> RoundedCornerShape(
            topStart = cornerRadiusDp,
            bottomStart = cornerRadiusDp,
            topEnd = 0.dp,
            bottomEnd = 0.dp
        )

        LocationHistorySelectedDayPosition.CENTER -> RoundedCornerShape(0.dp)
        LocationHistorySelectedDayPosition.END -> RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = cornerRadiusDp,
            bottomEnd = cornerRadiusDp
        )

        LocationHistorySelectedDayPosition.SOLO -> RoundedCornerShape(cornerRadiusDp)
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 42.dp)
            .clip(shape)
            .background(
                if (dayState.isSelected) MaterialTheme.colors.primary else Color.Transparent
            )
            .combinedClickable(
                onClick = {
                    onDaySelected(dayState.date)
                },
                onLongClick = {
                    onDayLongClick(dayState.date)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = dayState.isLoading,
//                    (dayState.position == LocationHistorySelectedDayPosition.SOLO || dayState.position == LocationHistorySelectedDayPosition.CENTER),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round,
                    color = if (dayState.isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .alpha(animateFloatAsState(if (dayState.isLoading) 0f else 1f).value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                style = MaterialTheme.typography.caption,
                text = dayState.formattedDay,
                color = if (dayState.isSelected) {
                    MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
                } else {
                    if (dayState.isWeekend) {
                        AppColors.redMain
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    }
                },
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Text(
                style = MaterialTheme.typography.subtitle1,
                text = dayState.day.toString(),
                color = if (dayState.isSelected)
                    MaterialTheme.colors.onPrimary
                else
                    MaterialTheme.colors.onSurface,
                fontSize = 14.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (dayState.formattedMonth != null) {
                Text(
                    style = MaterialTheme.typography.caption,
                    text = dayState.formattedMonth ?: "",
                    color = if (dayState.isSelected) {
                        MaterialTheme.colors.onPrimary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    },
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}