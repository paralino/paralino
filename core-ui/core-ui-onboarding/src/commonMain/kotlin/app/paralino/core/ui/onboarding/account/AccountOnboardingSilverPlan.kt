package app.paralino.core.ui.onboarding.account

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.ui.components.BlurredCircle
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.drawBlurredCircles
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountOnboardingSilverPlan(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = modifier
            .padding(vertical = 12.dp, horizontal = 20.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp,
            backgroundColor = MaterialTheme.colors.primary
        ) {
            Column(
                modifier = Modifier.padding(16.dp).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp).scale(scale),
                    text = "🎁",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(MR.strings.plans_gift_screen_box_title),
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(MR.strings.plans_gift_screen_box_message).annotatedBoldCompose(),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AccountOnboardingSilverPlanBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background)
    ) {
        val blurredCircles = remember {
            listOf(
                BlurredCircle(
                    center = Offset(x = 1.3f, y = 0.35f),
                    radius = 0.15f,
                    color = AppColors.accentDark.copy(alpha = 0.35f),
                    blurRadius = 0.12f
                ),
                BlurredCircle(
                    center = Offset(x = 0.65f, y = 0.3f),
                    radius = 0.25f,
                    color = AppColors.accentDark.copy(alpha = 0.2f),
                    blurRadius = 0.1f
                ),
                BlurredCircle(
                    center = Offset(x = 0f, y = 0.35f),
                    radius = 0.15f,
                    color = AppColors.accentDark.copy(alpha = 0.45f),
                    blurRadius = 0.2f
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBlurredCircles(blurredCircles)
        }

        Icon(
            painter = CoreAppIcons.StarFamily.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 140.dp)
                .size(150.dp)
                .rotate(20f)
                .offset(x = (-80).dp),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )
        Icon(
            painter = CoreAppIcons.StarFamily.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(200.dp)
                .rotate(-30f)
                .offset(x = (40).dp, y = 80.dp),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )
    }
}