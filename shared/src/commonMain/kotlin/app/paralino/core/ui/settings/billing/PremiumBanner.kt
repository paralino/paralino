package app.paralino.core.ui.settings.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.BlurredCircle
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.drawBlurredCircles
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.stringResource
import kotlin.random.Random


@Composable
fun PremiumPlanBannerSmall(
    modifier: Modifier = Modifier,
    elevation: Dp = 16.dp,
    description: String = stringResource(MR.strings.plans_banner_settings_description),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(12.dp),
                ambientColor = if (isSystemInDarkTheme()) AppColors.accentLight else AppColors.accentDark,
                spotColor = if (isSystemInDarkTheme()) AppColors.accentLight else AppColors.accentDark,
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colors.primary,
                        AppColors.accentDark
                    ),
                    center = Offset(x = 0f, y = 0.2f)
                )
            )
    ) {
        // Blurred circles background
        val blurredCircles = remember { generateBlurredCircles() }
        Canvas(modifier = Modifier.matchParentSize()) {
            drawBlurredCircles(blurredCircles)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(MR.strings.plans_banner_settings_title),
                    color = Color.White,
                    style = MaterialTheme.typography.button.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    color = Color.White,
                    style = MaterialTheme.typography.caption
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Icon(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = AppColors.lightBackground,
                        shape = CircleShape
                    )
                    .padding(4.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}

@Composable
fun PremiumPlanBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedCircles = animatePlanBannerCircles()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSystemInDarkTheme()) AppColors.accentLight else AppColors.accentDark,
                spotColor = if (isSystemInDarkTheme()) AppColors.accentLight else AppColors.accentDark,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colors.primary,
                        AppColors.accentDark
                    )
                )
            )
    ) {
        // Animated blurred circles background
        Canvas(modifier = Modifier.matchParentSize()) {
            drawBlurredCircles(animatedCircles)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(MR.strings.plans_banner_settings_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(end = 50.dp),
                text = stringResource(MR.strings.plans_banner_settings_description),
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClick)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(MR.strings.plans_banner_settings_action),
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedPlanStar(
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

private val circleAnimationRepeatCount = 4

@Composable
private fun animatePlanBannerCircles(): List<BlurredCircle> {
    val circles = generateBlurredCircles()
    return circles.mapIndexed { index, circle ->
        // Animatable values
        val positionX = remember { Animatable(circle.center.x) }
        val positionY = remember { Animatable(circle.center.y) }
        val radius = remember { Animatable(circle.radius) }
        val blurRadius = remember { Animatable(circle.blurRadius) }

        LaunchedEffect(Unit) {
            repeat(circleAnimationRepeatCount) {
                positionX.animateTo(
                    targetValue = circle.center.x + 0.25f,
                    animationSpec = tween(
                        Random.nextInt(4000, 6000),
                        easing = FastOutSlowInEasing
                    )
                )
                positionX.animateTo(
                    targetValue = circle.center.x,
                    animationSpec = tween(
                        Random.nextInt(4000, 6000),
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }

        LaunchedEffect(Unit) {
            repeat(circleAnimationRepeatCount) {
                positionY.animateTo(
                    targetValue = circle.center.y + 0.40f,
                    animationSpec = tween(
                        Random.nextInt(4000, 6000),
                        easing = FastOutSlowInEasing
                    )
                )
                positionY.animateTo(
                    targetValue = circle.center.y,
                    animationSpec = tween(
                        Random.nextInt(4000, 6000),
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }

        LaunchedEffect(Unit) {
            repeat(circleAnimationRepeatCount) {
                radius.animateTo(
                    targetValue = circle.radius * 1.2f,
                    animationSpec = tween(2000 + it * 500, easing = LinearEasing)
                )
                radius.animateTo(
                    targetValue = circle.radius,
                    animationSpec = tween(2000 + it * 500, easing = LinearEasing)
                )
            }
        }

        LaunchedEffect(Unit) {
            repeat(circleAnimationRepeatCount) {
                blurRadius.animateTo(
                    targetValue = circle.blurRadius * 1.4f,
                    animationSpec = tween(2500 + it * 500, easing = LinearEasing)
                )
                blurRadius.animateTo(
                    targetValue = circle.blurRadius,
                    animationSpec = tween(2500 + it * 500, easing = LinearEasing)
                )
            }
        }

        circle.copy(
            center = Offset(positionX.value, positionY.value),
            radius = radius.value,
            blurRadius = blurRadius.value
        )
    }
}


private val starAnimationRepeatCount = 4
private val starAnimationRoundDuration = 3000

@Composable
private fun AnimatedPlanStar(modifier: Modifier = Modifier) {
    val starRotation = remember { Animatable(0f) }
    val starScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        repeat(starAnimationRepeatCount) {
            starRotation.animateTo(
                targetValue = 30f,
                animationSpec = tween(starAnimationRoundDuration, easing = LinearEasing)
            )
            starRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(starAnimationRoundDuration, easing = LinearEasing)
            )
        }
    }

    LaunchedEffect(Unit) {
        repeat(starAnimationRepeatCount) {
            starScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(starAnimationRoundDuration, easing = LinearEasing)
            )
            starScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(starAnimationRoundDuration, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .offset(x = 30.dp)
    ) {
        ImageModelImage(
            imageModel = ImageModel.emoji(Emojis.star),
            modifier = Modifier
                .requiredSize(140.dp)
                .rotate(starRotation.value)
                .scale(starScale.value),
            useBackgroundForEmoji = false
        )
    }
}

private fun generateBlurredCircles(): List<BlurredCircle> {
    return listOf(
        BlurredCircle(
            center = Offset(x = 1.1f, y = 0.1f),
            radius = 0.4f,
            color = AppColors.highlightGold,
            blurRadius = 0.2f
        ),
        BlurredCircle(
            center = Offset(x = 0.45f, y = 0.7f),
            radius = 0.6f,
            color = AppColors.darkBackground,
            blurRadius = 0.08f
        ),
        BlurredCircle(
            center = Offset(x = 0.05f, y = 0f),
            radius = 0.25f,
            color = AppColors.lightThemePrimary,
            blurRadius = 0.12f
        )
    )
}
