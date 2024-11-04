package app.paralino.core.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import app.paralino.core.MR
import app.paralino.core.presentation.settings.billing.PlanInfoViewModel
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.BlurredCircle
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.drawBlurredCircles
import app.paralino.core.ui.settings.billing.PremiumPlanBannerSmall
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBold
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun FreePlanInfoScreenCore(
    coreUiState: CoreUiState,
    showBanner: Boolean,
    vm: PlanInfoViewModel,
    onContinue: () -> Unit,
    openPaywall: () -> Unit
) {
    MessageDispatcherEffect(vm, coreUiState)
    val uiState by vm.uiState.collectAsState()
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
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colors.background)
    ) {
        val blurredCircles = remember {
            listOf(
                BlurredCircle(
                    center = Offset(x = 1.3f, y = 0.45f),
                    radius = 0.3f,
                    color = AppColors.accentDark.copy(alpha = 0.35f),
                    blurRadius = 0.12f
                ),
                BlurredCircle(
                    center = Offset(x = 0.45f, y = 0.5f),
                    radius = 0.4f,
                    color = AppColors.accentDark.copy(alpha = 0.2f),
                    blurRadius = 0.08f
                ),
                BlurredCircle(
                    center = Offset(x = 0f, y = 0.55f),
                    radius = 0.25f,
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

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(MR.strings.plans_gift_screen_title),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    backgroundColor = MaterialTheme.colors.primary
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(MR.strings.plans_gift_screen_box_message),
                            style = MaterialTheme.typography.subtitle1,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val benefits = remember(uiState) {
                    uiState.planBenefits().joinToString(separator = "\n") {
                        "⭐️ ${it.content}"
                    }.annotatedBold()
                }
                Column {
                    Text(
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        text = stringResource(MR.strings.plans_gift_screen_benefits_title),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = benefits,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.padding(vertical = 16.dp).padding(horizontal = 8.dp)
                    )
                }
            }

            FilledButton(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                style = ButtonStyle.BIG,
                onClick = {
                    vm.confirmFreePlan()
                    onContinue()
                }
            ) {
                Text(text = stringResource(MR.strings.button_continue))
            }

            Spacer(modifier = Modifier.size(8.dp))

            if (showBanner) {
                Row(
                    modifier = Modifier.padding(horizontal = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Icon(
                        modifier = Modifier.padding(horizontal = 8.dp).size(14.dp),
                        painter = CoreAppIcons.ExpandMore.asPainterResource(),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier,
                        text = stringResource(MR.strings.plans_gift_screen_separator_text),
                    )
                    Icon(
                        modifier = Modifier.padding(horizontal = 8.dp).size(14.dp),
                        painter = CoreAppIcons.ExpandMore.asPainterResource(),
                        contentDescription = null
                    )
                    Divider(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.size(8.dp))
                PremiumPlanBannerSmall(
                    modifier = Modifier.padding(bottom = 30.dp).padding(horizontal = 20.dp),
                    elevation = 8.dp,
                    onClick = openPaywall
                )
            } else {
                Spacer(modifier = Modifier.size(30.dp))
            }
        }
    }
}
