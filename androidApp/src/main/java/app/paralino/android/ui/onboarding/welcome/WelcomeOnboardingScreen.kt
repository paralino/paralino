package app.paralino.android.ui.onboarding.welcome

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import dev.icerock.moko.resources.StringResource
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
        title = stringResource(step.title),
        info = step.info?.let { stringResource(it) },
        buttonText = if (step.next() == null) {
            stringResource(MR.strings.button_continue)
        } else {
            stringResource(MR.strings.button_next)
        },
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
        AnimatedContent(
            modifier = Modifier
                .padding(top = 20.dp, bottom = 12.dp)
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
                        .alpha(animateFloatAsState(if (step.titleTag != null) 1f else 0f).value)
                        .background(
                            color = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                Text(
                    text = step.titleTag?.let { stringResource(it) } ?: "",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        AnimatedContent(
            modifier = Modifier.zIndex(0f),
            targetState = step.parentKey,
            transitionSpec = onboardingContentTransitionSpec(),
            label = "animated header"
        ) {
            when (it) {
                200 -> WelcomeOnboardingPagePlaces()
                300 -> WelcomeOnboardingPageControl()
                else -> WelcomeOnboardingPageLocation(isEncryptionOn = step == WelcomeOnboardingContent.STEP_LOCATION_ENCRYPTION)
            }
        }
    }
}

enum class WelcomeOnboardingContent(
    val parentKey: Int,
    val title: StringResource,
    val titleTag: StringResource?,
    val info: StringResource?
) {
    STEP_LOCATION(
        parentKey = 100,
        title = MR.strings.onboarding_step_location_title,
        titleTag = MR.strings.onboarding_step_location_title_tag,
        info = MR.strings.onboarding_step_location_info
    ),
    STEP_LOCATION_ENCRYPTION(
        parentKey = 100,
        title = MR.strings.onboarding_step_location_encryption_title,
        titleTag = MR.strings.onboarding_step_location_encryption_title_tag,
        info = MR.strings.onboarding_step_location_encryption_info
    ),
    STEP_PLACES(
        parentKey = 200,
        title = MR.strings.onboarding_step_places_title,
        titleTag = MR.strings.onboarding_step_places_title_tag,
        info = MR.strings.onboarding_step_places_info
    ),
    STEP_OPTIONS(
        parentKey = 300,
        title = MR.strings.onboarding_step_options_title,
        titleTag = MR.strings.onboarding_step_options_title_tag,
        info = MR.strings.onboarding_step_options_info
    );

    fun next(): WelcomeOnboardingContent? {
        return entries.getOrNull(index = this.ordinal + 1)
    }

    fun previous(): WelcomeOnboardingContent? {
        return entries.getOrNull(index = this.ordinal - 1)
    }
}
