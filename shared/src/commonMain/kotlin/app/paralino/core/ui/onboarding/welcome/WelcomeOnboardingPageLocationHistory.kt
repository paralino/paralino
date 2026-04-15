package app.paralino.core.ui.onboarding.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.MapMarkerContent
import app.paralino.core.ui.places.PlacesEmptyStateMarkers
import app.paralino.core.ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
fun WelcomeOnboardingPageLocationHistory(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp, top = 12.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        HistoryMapOnboardingContent(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun HistoryMapOnboardingContent(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    rootColor: Color = MaterialTheme.colors.surface,
    backgroundColor: Color = MaterialTheme.colors.surface,
    streetColor: Color = MaterialTheme.colors.secondary.copy(alpha = 0.4f),
) {

    // Animation progress from 0 to 1 and back
    val animationProgress = if (animate) {
        val infiniteTransition = rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else {
        1f
    }

    // Street map path coordinates (normalized 0-1)
    val pathPoints = remember {
        listOf(
            Offset(0.1f, 0.8f),  // Start bottom-left
            Offset(0.1f, 0.6f),
            Offset(0.3f, 0.6f),
            Offset(0.3f, 0.5f),
            Offset(0.3f, 0.2f),
            Offset(0.5f, 0.2f),
            Offset(0.5f, 0.3f),
            Offset(0.6f, 0.3f),
            Offset(0.6f, 0.5f),
            Offset(0.8f, 0.5f),
            Offset(0.8f, 0.3f),
            Offset(0.8f, 0.29f),
        )
    }

    // Pin locations (where the path turns)
    val pinLocations = remember {
        listOf(
            Offset(0.1f, 0.8f), // Start bottom-left
            Offset(0.1f, 0.6f),
            Offset(0.3f, 0.6f),
            Offset(0.3f, 0.5f),
            Offset(0.3f, 0.2f),
            Offset(0.5f, 0.2f),
            Offset(0.5f, 0.3f),
            Offset(0.6f, 0.3f),
            Offset(0.6f, 0.5f),
            Offset(0.8f, 0.5f),
            Offset(0.8f, 0.3f),
            Offset(0.8f, 0.3f),
        )
    }

    val markers = remember {
        PlacesEmptyStateMarkers()
    }

    Box(
        modifier = modifier
    ) {
        // Street map canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawStreetMap(
                width = size.width,
                height = size.height,
                streetColor = streetColor
            )

            if (backgroundColor.alpha < 1f) {
                drawSquareGradientBox(
                    color = rootColor
                )
                drawSquareGradientBox(
                    color = backgroundColor
                )
            } else {
                drawSquareGradientBox(
                    color = backgroundColor
                )
            }

            drawPathWithProgress(
                pathPoints = pathPoints,
                progress = animationProgress,
                width = size.width,
                height = size.height
            )
        }

        // Pin icons at turning points - appear as marker reaches them
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Calculate which pins should be visible based on progress
            val totalSegments = pathPoints.size
            val currentSegmentFloat = animationProgress * totalSegments
            val visiblePinCount = (currentSegmentFloat + 0f)
                .roundToInt()
                .coerceAtMost(pinLocations.size)

            pinLocations.take(visiblePinCount).forEach { pinLocation ->
                val xOffset = (pinLocation.x * maxWidth.value - 10).dp // 10dp is half of icon size
                val yOffset = (pinLocation.y * maxHeight.value - 20).dp // 10dp is half of icon size

                Icon(
                    painter = CoreAppIcons.LocationOnFilled.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = xOffset, y = yOffset),
                    tint = Color.Unspecified
                )
            }
        }

        // Animated marker following the path
        AnimatedMarker(
            pathPoints = pathPoints,
            progress = animationProgress,
            markerState = markers.peter
        )

//        SquareGradientBox(
//            modifier = Modifier.fillMaxSize()
//        )
    }
}

@Composable
private fun AnimatedMarker(
    pathPoints: List<Offset>,
    progress: Float,
    markerState: MapPinData // Using the same marker state as location page
) {
    // Calculate current position along the path
    val currentPosition = remember(progress) {
        calculatePositionOnPath(pathPoints, progress)
    }

    MapMarkerContent(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = (currentPosition.x * constraints.maxWidth - placeable.width / 2).toInt()
                    val y =
                        (currentPosition.y * constraints.maxHeight - placeable.height).toInt() // Anchor to bottom center
                    placeable.place(x, y)
                }
            },
        state = markerState
    )
}

private fun DrawScope.drawStreetMap(
    width: Float,
    height: Float,
    streetColor: Color
) {
    val strokeWidth = 3.dp.toPx()

    // Horizontal streets
    val horizontalStreets = listOf(0.2f, 0.3f, 0.5f, 0.6f, 0.8f)
    horizontalStreets.forEach { y ->
        drawLine(
            color = streetColor,
            start = Offset(0f, y * height),
            end = Offset(width, y * height),
            strokeWidth = strokeWidth
        )
    }

    // Vertical streets
    val verticalStreets = listOf(0.1f, 0.3f, 0.5f, 0.6f, 0.8f)
    verticalStreets.forEach { x ->
        drawLine(
            color = streetColor,
            start = Offset(x * width, 0f),
            end = Offset(x * width, height),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawPathWithProgress(
    pathPoints: List<Offset>,
    progress: Float,
    width: Float,
    height: Float
) {
    if (pathPoints.size < 2) return

    val path = Path()
    val scaledPoints = pathPoints.map {
        Offset(it.x * width, it.y * height)
    }

    path.moveTo(scaledPoints[0].x, scaledPoints[0].y)

    // Calculate how much of the path to draw based on progress
    val totalSegments = scaledPoints.size - 1
    val currentSegment = (progress * totalSegments).toInt().coerceAtMost(totalSegments - 1)
    val segmentProgress = (progress * totalSegments) - currentSegment

    // Draw completed segments
    for (i in 1..currentSegment) {
        path.lineTo(scaledPoints[i].x, scaledPoints[i].y)
    }

    // Draw partial segment
    if (currentSegment < totalSegments - 1) {
        val start = scaledPoints[currentSegment]
        val end = scaledPoints[currentSegment + 1]
        val partialEnd = Offset(
            start.x + (end.x - start.x) * segmentProgress,
            start.y + (end.y - start.y) * segmentProgress
        )
        path.lineTo(partialEnd.x, partialEnd.y)
    }

    drawPath(
        path = path,
        color = AppColors.accentDark,
        style = Stroke(
            width = 6.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun calculatePositionOnPath(pathPoints: List<Offset>, progress: Float): Offset {
    if (pathPoints.isEmpty()) return Offset.Zero
    if (pathPoints.size == 1) return pathPoints[0]
    if (progress <= 0f) return pathPoints[0]
    if (progress >= 1f) return pathPoints.last()

    val totalSegments = pathPoints.size - 1
    val scaledProgress = progress * totalSegments
    val segmentIndex = scaledProgress.toInt().coerceAtMost(totalSegments - 1)
    val segmentProgress = scaledProgress - segmentIndex

    val start = pathPoints[segmentIndex]
    val end = pathPoints[segmentIndex + 1]

    return Offset(
        start.x + (end.x - start.x) * segmentProgress,
        start.y + (end.y - start.y) * segmentProgress
    )
}

fun DrawScope.drawSquareGradientBox(
    color: Color,
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color, Color.Transparent, Color.Transparent, color),
        )
    )

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(color, Color.Transparent, Color.Transparent, color),
        )
    )
}