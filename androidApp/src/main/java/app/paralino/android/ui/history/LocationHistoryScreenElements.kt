package app.paralino.android.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle


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

        Column(
            modifier = Modifier.padding(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    cardHeight(with(density) { coordinates.size.height.toDp() })
                }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp)
                ) {
                    LocationHistoryDetailCard(
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally)
                ) {
                    this.item(key = "stats") {
                        MapFocusTypeTab(
                            modifier = Modifier,
                            selected = uiState.mapFocusType == LocationHistoryMapFocusType.Stats,
                            name = "Stats",
                            onClick = {
                                onEvent(
                                    LocationHistoryUiEvent.ChangeMapFocusType(
                                        LocationHistoryMapFocusType.Stats
                                    )
                                )
                            }
                        )
                    }
                    this.item(key = "point") {
                        MapFocusTypeTab(
                            modifier = Modifier,
                            selected = uiState.mapFocusType == LocationHistoryMapFocusType.LocationPoint,
                            name = "Point",
                            onClick = {
                                onEvent(
                                    LocationHistoryUiEvent.ChangeMapFocusType(
                                        LocationHistoryMapFocusType.LocationPoint
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapFocusTypeTab(
    modifier: Modifier = Modifier,
    selected: Boolean,
    name: String,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onSurface
    )
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colors.primary
        else MaterialTheme.colors.surface
    )

    DataInfoChip(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        backgroundColor = backgroundColor,
        imageOrIcon = null,
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