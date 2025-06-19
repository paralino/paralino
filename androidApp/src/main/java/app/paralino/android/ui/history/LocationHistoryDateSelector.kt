package app.paralino.android.ui.history

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.presentation.history.LocationHistoryDayState
import app.paralino.core.presentation.history.LocationHistorySelectedDayPosition
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.onboarding.onboardingContentTransitionSpec
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun LocationHistoryDateSelector(
    modifier: Modifier = Modifier,
    dayRange: Int,
    days: List<LocationHistoryDayState>,
    onEvent: (LocationHistoryUiEvent) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Scrollable days container
        Card(
            modifier = Modifier
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            backgroundColor = MaterialTheme.colors.background,
            elevation = 4.dp
        ) {
            val scrollState = rememberLazyListState(0)
            val scope = rememberCoroutineScope()
            LazyRow(
                modifier = Modifier,
                contentPadding = PaddingValues(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                reverseLayout = true,
                state = scrollState
            ) {
                // Day cells
                itemsIndexed(
                    items = days,
                    key = { index, item -> item.date.toString() }
                ) { index, dayState ->
                    DayCell(
                        dayState = dayState,
                        onDaySelected = {
                            scope.launch { scrollState.animateScrollToItem(index = (index - 1).coerceAtLeast(0)) }
                            onEvent(LocationHistoryUiEvent.OnSelectDay(it))
                        }
                    )
                }

                // Load previous month arrow
                item {
                    Box(
                        modifier = Modifier
                            .clickable { onEvent(LocationHistoryUiEvent.OnPreviousMonth) }
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
            }
        }

        // Day range button
        Column(
            modifier = Modifier
                .size(width = 50.dp, height = 48.dp)
                .clickable { onEvent(LocationHistoryUiEvent.ToggleDayRange) }
                .shadow(elevation = 4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = dayRange,
                label = "animated count",
                transitionSpec = onboardingContentTransitionSpec()
            ) {
                Text(
                    style = MaterialTheme.typography.caption,
                    text = "${it}x",
                    color = MaterialTheme.colors.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    dayState: LocationHistoryDayState,
    onDaySelected: (LocalDate) -> Unit
) {
    // Animate corner radius based on position
    val cornerRadius = 8.dp

    val shape = when (dayState.position) {
        LocationHistorySelectedDayPosition.START -> RoundedCornerShape(
            topStart = cornerRadius,
            bottomStart = cornerRadius,
            topEnd = 0.dp,
            bottomEnd = 0.dp
        )

        LocationHistorySelectedDayPosition.CENTER -> RoundedCornerShape(0.dp)
        LocationHistorySelectedDayPosition.END -> RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = cornerRadius,
            bottomEnd = cornerRadius
        )

        LocationHistorySelectedDayPosition.SOLO -> RoundedCornerShape(cornerRadius)
    }

    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 40.dp)
            .clip(shape)
            .background(
                if (dayState.isSelected) MaterialTheme.colors.primary else Color.Transparent
            )
            .clickable { onDaySelected(dayState.date) },
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
        Text(
            style = MaterialTheme.typography.caption,
            text = dayState.formattedMonth,
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationHistoryDateSelectorPreview() {
    val sampleDays = listOf(
        LocationHistoryDayState(
            isSelected = false,
            date = LocalDate(2024, 1, 15),
            formattedDay = "Mon",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = true,
            date = LocalDate(2024, 1, 16),
            formattedDay = "Tue",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.START,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = true,
            date = LocalDate(2024, 1, 17),
            formattedDay = "Wed",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.CENTER,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = true,
            date = LocalDate(2024, 1, 18),
            formattedDay = "Thu",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.END,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = false,
            date = LocalDate(2024, 1, 19),
            formattedDay = "Fri",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = false
        ),
        LocationHistoryDayState(
            isSelected = false,
            date = LocalDate(2024, 1, 20),
            formattedDay = "Sat",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = true
        ),
        LocationHistoryDayState(
            isSelected = false,
            date = LocalDate(2024, 1, 21),
            formattedDay = "Sun",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = true
        ),
        LocationHistoryDayState(
            isSelected = true,
            date = LocalDate(2024, 1, 22),
            formattedDay = "Mon",
            formattedMonth = "Jan",
            position = LocationHistorySelectedDayPosition.SOLO,
            isWeekend = false
        )
    )

    AppTheme {
        Surface {
            LocationHistoryDateSelector(
                modifier = Modifier.padding(16.dp),
                dayRange = 7,
                days = sampleDays.reversed(),
                onEvent = { },
            )
        }
    }
}