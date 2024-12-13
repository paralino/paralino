package app.paralino.core.ui.onboarding.welcome

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.presentation.onboarding.WelcomeOnboardingViewModel
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
import app.paralino.core.ui.onboarding.onboardingContentTransitionSpec
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun WelcomeOnboardingScreenCore(
    vm: WelcomeOnboardingViewModel,
    isDemo: Boolean,
    step: MutableState<WelcomeOnboardingContent> = rememberSaveable {
        mutableStateOf(WelcomeOnboardingContent.STEP_LOCATION)
    },
    onFinish: () -> Unit
) {

    WelcomeOnboardingScreen(
        step = step.value,
        onNext = {
            val next = step.value.next()
            if (next != null) {
                step.value = next
            } else {
                if (!isDemo) {
                    vm.completeOnboarding()
                }
                onFinish()
            }
        },
        onNewRandomString = {
            vm.provideRandomString()
        }
    )
}

@Composable
private fun WelcomeOnboardingScreen(
    step: WelcomeOnboardingContent,
    onNext: () -> Unit,
    onNewRandomString: () -> String,
) {
    BaseOnboardingScreen(
        modifier = Modifier
            .fillMaxSize(),
        order = step.titleKey,
        title = stringResource(step.title),
        info = step.info?.let { stringResource(it) },
        buttonText = stringResource(step.buttonText),
        onSkip = null,
        onNext = onNext,
        headerContent = {
            WelcomeOnboardingHeaderContent(step = step, onNewRandomString = onNewRandomString)
        }
    )
}

@Composable
private fun WelcomeOnboardingHeaderContent(
    modifier: Modifier = Modifier,
    step: WelcomeOnboardingContent,
    onNewRandomString: () -> String,
) {
    Column(modifier = modifier) {
        AnimatedContent(
            modifier = Modifier
                .padding(top = 20.dp)
                .padding(horizontal = 20.dp)
                .zIndex(1f),
            targetState = step.titleKey,
            label = "animated info",
            transitionSpec = onboardingContentTransitionSpec(fixedTransitionHeight = 50)
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
            transitionSpec = onboardingContentTransitionSpec(fixedTransitionHeight = 100),
            label = "animated header"
        ) {
            when (WelcomeOnboardingContent.fromParentKey(it)) {
                WelcomeOnboardingContent.STEP_PLACES -> {
                    WelcomeOnboardingPagePlaces()
                }
                WelcomeOnboardingContent.STEP_OPTIONS,
                WelcomeOnboardingContent.STEP_OPTIONS_2 -> {
                    WelcomeOnboardingPageControl(
                        modifier = Modifier.padding(top = 12.dp),
                        animationStarted = step == WelcomeOnboardingContent.STEP_OPTIONS_2
                    )
                }

                WelcomeOnboardingContent.STEP_LOCATION,
                WelcomeOnboardingContent.STEP_LOCATION_ENCRYPTION,
                null -> {
                    WelcomeOnboardingPageLocation(
                        modifier = Modifier.padding(top = 12.dp),
                        isEncryptionOn = step == WelcomeOnboardingContent.STEP_LOCATION_ENCRYPTION,
                        onNewRandomString = onNewRandomString
                    )
                }
            }
        }
    }
}

enum class WelcomeOnboardingContent(
    val parentKey: Int,
    val titleKey: Int,
    val titleTag: StringResource?,
    val title: StringResource,
    val info: StringResource?,
    val buttonText: StringResource
) {
    STEP_LOCATION(
        parentKey = 100,
        titleKey = 100,
        titleTag = MR.strings.onboarding_step_location_title_tag,
        title = MR.strings.onboarding_step_location_title,
        info = MR.strings.onboarding_step_location_info,
        buttonText = MR.strings.button_next
    ),
    STEP_LOCATION_ENCRYPTION(
        parentKey = 100,
        titleKey = 101,
        titleTag = MR.strings.onboarding_step_location_encryption_title_tag,
        title = MR.strings.onboarding_step_location_encryption_title,
        info = MR.strings.onboarding_step_location_encryption_info,
        buttonText = MR.strings.button_next
    ),
    STEP_PLACES(
        parentKey = 200,
        titleKey = 200,
        titleTag = MR.strings.onboarding_step_places_title_tag,
        title = MR.strings.onboarding_step_places_title,
        info = MR.strings.onboarding_step_places_info,
        buttonText = MR.strings.button_next
    ),
    STEP_OPTIONS(
        parentKey = 300,
        titleKey = 300,
        titleTag = MR.strings.onboarding_step_options_title_tag,
        title = MR.strings.onboarding_step_options_title,
        info = MR.strings.onboarding_step_options_info,
        buttonText = MR.strings.button_next
    ),
    STEP_OPTIONS_2(
        parentKey = 300,
        titleKey = 300,
        titleTag = MR.strings.onboarding_step_options_title_tag,
        title = MR.strings.onboarding_step_options_title,
        info = MR.strings.onboarding_step_options_info,
        buttonText = MR.strings.button_continue
    );

    fun next(): WelcomeOnboardingContent? {
        return entries.getOrNull(index = this.ordinal + 1)
    }

    fun previous(): WelcomeOnboardingContent? {
        return when (this) {
            STEP_OPTIONS_2 -> entries.getOrNull(index = this.ordinal - 2)
            else -> entries.getOrNull(index = this.ordinal - 1)
        }
    }

    companion object {
        fun fromParentKey(parentKey: Int): WelcomeOnboardingContent? {
            return entries.firstOrNull { it.parentKey == parentKey }
        }
    }
}
