package app.paralino.android.ui.onboarding.welcome

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
    var step by rememberSaveable { mutableStateOf<WelcomeOnboardingContent>(WelcomeOnboardingContent.STEP_LOCATION) }

    BackHandler(step.ordinal > 0) {
        val previous = step.previous()
        if (previous != null) {
            step = previous
        }
    }

    BaseOnboardingScreen(
        modifier = Modifier
            .fillMaxSize(),
        order = step.ordinal,
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

@SuppressLint("UnusedContentLambdaTargetStateParameter")
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
                targetState = step.ordinal,
                label = "animated info",
                transitionSpec = onboardingContentTransitionSpec()
            ) {
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
                        text = step.titleTag,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
        }
        AnimatedContent(
            modifier = Modifier.zIndex(0f),
            targetState = step.parentKey,
            transitionSpec = onboardingContentTransitionSpec(),
            label = "animated header"
        ) {
            when (it) {
                2 -> WelcomeOnboardingPage2()
                3 -> WelcomeOnboardingPage3()
                else -> WelcomeOnboardingPage1(isEncryptionOn = step == WelcomeOnboardingContent.STEP_LOCATION_ENCRYPTION)
            }
        }
    }
}

enum class WelcomeOnboardingContent(
    val parentKey: Int,
    val title: String,
    val titleTag: String?,
    val info: String?
) {
    STEP_LOCATION(
        parentKey = 1,
        title = "Real-time location sharing",
        titleTag = "Paralino",
        info = "Battery, speed, elevation and network monitoring. Private groups with family, friends or only your devices."
    ),
    STEP_LOCATION_ENCRYPTION(
        parentKey = 1,
        title = "With privacy",
        titleTag = "Encryption",
        info = "Everything always safe and encrypted."
    ),
    STEP_PLACES(
        parentKey = 2,
        title = "Place alerts",
        titleTag = "Simplicity",
        info = "Get notified when someone enters/leaves predefined area"
    ),
    STEP_OPTIONS(
        parentKey = 3,
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
