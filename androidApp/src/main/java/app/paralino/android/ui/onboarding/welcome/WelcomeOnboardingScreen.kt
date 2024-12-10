package app.paralino.android.ui.onboarding.welcome

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import dev.icerock.moko.resources.compose.stringResource

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun WelcomeOnboardingScreen(
    coreUiState: CoreUiState,
    onNext: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf<WelcomeOnboardingContent>(WelcomeOnboardingContent.STEP_1) }

    BackHandler(step.order > 1) {
        val previous = step.previous()
        if (previous != null) {
            step = previous
        }
    }

    BaseOnboardingScreen(
        modifier = Modifier
            .fillMaxSize(),
        title = step.title,
        info = step.info,
        buttonText = stringResource(MR.strings.button_continue),
        onSkip = null,
        onNext = {
            val next = step.next()
            if (next != null) {
                step = next
            } else {
                onNext()
            }
        },
        headerContent = {
            WelcomeOnboardingHeaderContent(step = step)
        }
    )
}

@Composable
private fun WelcomeOnboardingHeaderContent(
    modifier: Modifier = Modifier,
    step: WelcomeOnboardingContent
) {
    Column(modifier = modifier) {
        if (step.titleTag != null) {
            AnimatedContent(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(horizontal = 20.dp)
                    .zIndex(1f),
                targetState = step.titleTag,
                label = "animated info"
            ) { contentText ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = CoreAppIcons.LauncherLogo.asPainterResource(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colors.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    Text(
                        text = contentText,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
        }
        AnimatedContent(
            modifier = Modifier.zIndex(0f),
            targetState = step.order,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                } else {
                    slideInVertically { height -> -height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                }.using(
                    // Disable clipping since the faded slide-in/out should
                    // be displayed out of bounds.
                    SizeTransform(clip = false)
                )
            }, label = "animated header"
        ) {
            when (it) {
                2 -> WelcomeOnboardingPage2()
                3 -> WelcomeOnboardingPage3()
                else -> WelcomeOnboardingPage1()
            }
        }
    }
}

enum class WelcomeOnboardingContent(
    val order: Int,
    val title: String,
    val titleTag: String?,
    val info: String?
) {
    STEP_1(
        order = 1,
        title = "Real-time location sharing",
        titleTag = "Privacy",
        info = "In private groups where all your data is always safe and for your eyes only"
    ),
    STEP_2(
        order = 2,
        title = "Place alerts",
        titleTag = "Simplicity",
        info = "Get notified when someone enters/leaves predefined area"
    ),
    STEP_3(
        order = 3,
        title = "Full sharing control",
        titleTag = "Control",
        info = "You are always in control with whom, how and what you share"
    );

    fun next(): WelcomeOnboardingContent? {
        return entries.getOrNull(index = this.ordinal + 1)
    }

    fun previous(): WelcomeOnboardingContent? {
        return entries.getOrNull(index = this.ordinal - 1)
    }
}
