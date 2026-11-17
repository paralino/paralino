package app.paralino.core.ui.privacyzones

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.common.GroupStatusTagType
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MapMarkerContent
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.groups.components.GroupStatusTag
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PrivacyZonesEmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(bottom = 20.dp)
    ) {

        PrivacyZonesEmptyStateMapContent(
            modifier = Modifier
                .padding(top = 20.dp)
                .padding(horizontal = 20.dp),
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier.padding(20.dp),
                text = stringResource(MR.strings.privacy_zones_empty_state_info).annotatedBoldCompose(),
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
        }

        NoticeInfoChip(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(vertical = 20.dp),
            text = stringResource(MR.strings.privacy_zones_empty_state_encryption_info),
        )
    }
}

@Composable
fun PrivacyZonesEmptyStateMapContent(
    modifier: Modifier = Modifier
) {
    var comparisonProgress by rememberSaveable { mutableFloatStateOf(0f) }
    val animation = Animatable(initialValue = 0f)
    LaunchedEffect(Unit) {
        delay(500)
        animation.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(durationMillis = 800),
            block = {
                comparisonProgress = this.value
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .height(350.dp)
            .dropShadow(
                shape = RoundedCornerShape(20.dp),
                shadow = Shadow(
                    radius = 12.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                )
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colors.background)
            .padding(6.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
        ) {
            Image(
                painter = if (isSystemInDarkTheme()) {
                    CoreAppIcons.MapMockupDark.asPainterResource()
                } else {
                    CoreAppIcons.MapMockupLight.asPainterResource()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            val markers = remember {
                PrivacyZonesEmptyStateMarkers()
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clipToBounds()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .matchParentSize()
                        .clipToBounds()
                ) {
                    val protectedRevealFraction = comparisonProgress

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(
                                ZoneRectangleShape(
                                    isLeftSide = false,
                                    percentage = protectedRevealFraction
                                )
                            )
                    ) {
                        PrivacyZonesEmptyScreenActualContent(
                            modifier = Modifier
                                .align(Alignment.Center),
                            state = markers.peter,
                        )

                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                            text = stringResource(MR.strings.privacy_zones_empty_state_map_label_your_location),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(
                                ZoneRectangleShape(
                                    isLeftSide = true,
                                    percentage = protectedRevealFraction
                                )
                            )
                    ) {
                        PrivacyZonesEmptyScreenProtectedContent(
                            modifier = Modifier.matchParentSize(),
                            state = markers.zone,
                        )

                        Text(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            text = stringResource(MR.strings.privacy_zones_empty_state_map_label_others_view),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        )
                    }

                    val dividerOffset = maxWidth * protectedRevealFraction
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = dividerOffset - 1.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colors.onBackground)
                    )
                }
            }
        }

        PrivacyZonesEmptyScreenComparisonSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            progress = comparisonProgress,
            onProgressChange = { comparisonProgress = it },
        )
    }
}

@Composable
private fun PrivacyZonesEmptyScreenComparisonSlider(
    modifier: Modifier = Modifier,
    progress: Float,
    onProgressChange: (Float) -> Unit,
) {
    val thumbWidth = 32.dp
    val thumbHeight = 36.dp
    val trackShape = RoundedCornerShape(14.dp)
    val thumbShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .height(42.dp)
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 3.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val density = LocalDensity.current
            val trackWidthPx = with(density) { maxWidth.toPx() }
            val thumbWidthPx = with(density) { thumbWidth.toPx() }
            val maxThumbOffsetPx = (trackWidthPx - thumbWidthPx).coerceAtLeast(0f)
            val thumbOffsetPx = progress.coerceIn(0f, 1f) * maxThumbOffsetPx

            val draggableState = rememberDraggableState { delta ->
                val nextOffset = (thumbOffsetPx + delta).coerceIn(0f, maxThumbOffsetPx)
                onProgressChange(
                    if (maxThumbOffsetPx == 0f) 0.5f else nextOffset / maxThumbOffsetPx
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(trackShape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = draggableState,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(trackShape),
                ) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                            .size(width = thumbWidth, height = thumbHeight)
                            .align(Alignment.CenterStart)
                            .clip(thumbShape)
                            .background(MaterialTheme.colors.primary)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = draggableState,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = CoreAppIcons.DragIndicator.asPainterResource(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp),
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyZonesEmptyScreenActualContent(
    modifier: Modifier = Modifier,
    state: MapPinData
) {
    val orbitProgress by rememberInfiniteTransition(label = "markerOrbit").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "markerOrbitProgress",
    )

    val orbitRadius = 20.dp
    val angle = orbitProgress * 2f * PI.toFloat()
    val xOffset = (cos(angle) * orbitRadius.value).dp
    val yOffset = (sin(angle) * orbitRadius.value).dp

    MapMarkerContent(
        modifier = modifier
            .offset(x = 60.dp)
            .offset(x = xOffset, y = yOffset),
        state = state,
    )
}

@Composable
private fun PrivacyZonesEmptyScreenProtectedContent(
    modifier: Modifier = Modifier,
    state: MapPinData
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val size = min(this.maxWidth, this.maxHeight)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isSystemInDarkTheme()) {
                        AppColors.accent.copy(alpha = 0.15f)
                    } else {
                        Color.Blue.copy(alpha = 0.15f)
                    }
                )
        )
        MapMarkerContent(state = state)

        val zoneMessages = remember {
            listOf(
                MR.strings.privacy_zones_empty_state_map_label_zoneMessage_1,
                MR.strings.privacy_zones_empty_state_map_label_zoneMessage_2,
                MR.strings.group_members_status_privacy_zone,
                MR.strings.privacy_zones_empty_state_map_label_zoneMessage_3,
                MR.strings.privacy_zones_empty_state_map_label_zoneMessage_4,
                MR.strings.group_members_status_location_hidden,
                MR.strings.privacy_zones_empty_state_map_label_zoneMessage_5,
                MR.strings.privacy_zones_empty_state_map_label_zoneMessage_6,
            )
        }
        var currentMessageIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(zoneMessages) {
            while (true) {
                delay(1500.milliseconds)
                currentMessageIndex = (currentMessageIndex + 1) % zoneMessages.size
            }
        }

        AnimatedContent(
            modifier = Modifier.offset(y = 30.dp),
            targetState = currentMessageIndex,
            label = "animated tag",
        ) { messageIndex ->
            GroupStatusTag(
                modifier = Modifier,
                status = GroupStatusTagType.SHARING_ACTIVE_PRIVACY_ZONE,
                extraMessage = stringResource(zoneMessages[messageIndex])
            )
        }
    }
}

@Stable
private class ZoneRectangleShape(
    val percentage: Float,
    val isLeftSide: Boolean
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline.Generic {
        val path = Path().apply {
            lineTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            lineTo(0f, 0f)
            close()
        }

        path.translate(
            Offset(
                x = if (isLeftSide) {
                    size.width * percentage - size.width
                } else {
                    size.width * percentage
                },
                y = 0f
            )
        )

        return Outline.Generic(path)
    }
}


private data class PrivacyZonesEmptyStateMarkers(
    val peter: MapPinData = MapPinData(
        groupOrSpaceId = "".asGroupOrSpaceId(),
        markerId = "peter",
        pinType = MapPinType.MEMBER,
        markerStyle = MapMarkerStyle.PIN,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = "Peter",
        image = ImageModel.emoji(Emojis.goat),
        lat = 0.0,
        lng = 0.0,
        accuracy = 10f,
        isApproximate = false,
        shouldShowApproximateCircle = false
    ),
    val zone: MapPinData = MapPinData(
        groupOrSpaceId = "".asGroupOrSpaceId(),
        markerId = "peter_zone",
        pinType = MapPinType.MEMBER,
        markerStyle = MapMarkerStyle.CIRCLE,
        focused = false,
        active = true,
        obsolete = false,
        hidden = false,
        title = "Secret",
        image = ImageModel.emoji(Emojis.goat),
        lat = 0.0,
        lng = 0.0,
        accuracy = 300f,
        isApproximate = true,
        shouldShowApproximateCircle = true
    )
)