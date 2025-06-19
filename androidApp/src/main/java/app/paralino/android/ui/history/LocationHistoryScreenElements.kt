package app.paralino.android.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.DeviceType
import app.paralino.core.presentation.history.LocationHistoryMapFocusType
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.theme.AppColors


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
            Box(modifier = Modifier.padding(start = 4.dp)) {
                ToolbarButtons.Back(elevation = 8.dp) { onEvent(LocationHistoryUiEvent.OnCloseScreen) }
            }
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

            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
                    .size(width = 90.dp, height = 36.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colors.background)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MapFocusTypeTab(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    selected = uiState.mapFocusType == LocationHistoryMapFocusType.Stats,
                    name = "Stats",
                    icon = CoreAppIcons.Home.asPainterResource(),
                    onClick = {
                        onEvent(
                            LocationHistoryUiEvent.ChangeMapFocusType(
                                LocationHistoryMapFocusType.Stats
                            )
                        )
                    }
                )

                MapFocusTypeTab(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    selected = uiState.mapFocusType == LocationHistoryMapFocusType.LocationPoint,
                    name = "Point",
                    icon = CoreAppIcons.Timeline.asPainterResource(),
                    onClick = {
                        onEvent(
                            LocationHistoryUiEvent.ChangeMapFocusType(
                                LocationHistoryMapFocusType.LocationPoint
                            )
                        )
                    }
                )
            }

            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    cardHeight(with(density) { coordinates.size.height.toDp() })
                }
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 12.dp)
                ) {
                    LocationHistoryDetailCard(
                        uiState = uiState,
                        onEvent = onEvent
                    )
                }

                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LocationHistoryDateSelector(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        dayRange = uiState.dateRangeDays,
                        days = uiState.displayedDays,
                        onEvent = onEvent
                    )
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
    icon: Painter,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onBackground
    )
    val backgroundColor by animateColorAsState(
//        if (selected) MaterialTheme.colors.secondary.copy(alpha = 0.5f)
        if (selected) MaterialTheme.colors.primary
        else MaterialTheme.colors.background
    )

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
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

//        Text(
//            modifier = Modifier.padding(start = 4.dp),
//            text = name,
//            color = contentColor,
//            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
//            fontSize = 12.sp,
//            lineHeight = 14.sp,
//            textAlign = TextAlign.Center
//        )
    }
}