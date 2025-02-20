package app.paralino.core.ui.onboarding.welcome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.onboarding.WelcomeOnboardingViewModel
import app.paralino.core.ui.onboarding.BaseOnboardingHeaderContent
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
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
    BaseOnboardingScreen(
        modifier = Modifier
            .fillMaxSize(),
        order = step.value.titleKey,
        title = stringResource(step.value.title),
        info = step.value.info?.let { stringResource(it) },
        buttonText = stringResource(step.value.buttonText),
        skipButtonText = null,
        contentSizeRatio = 0.3f,
        minHeaderSize = 400.dp,
        onSkip = null,
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
        headerContent = {
            BaseOnboardingHeaderContent(
                titleKey = step.value.titleKey,
                animatedContentKey = step.value.parentKey,
                titleTag = step.value.titleTag?.let { stringResource(it) },
                content = {
                    when (WelcomeOnboardingContent.fromParentKey(it)) {
                        WelcomeOnboardingContent.STEP_PLACES -> {
                            WelcomeOnboardingPagePlaces()
                        }
                        WelcomeOnboardingContent.STEP_OPTIONS,
                        WelcomeOnboardingContent.STEP_OPTIONS_2 -> {
                            WelcomeOnboardingPageControl(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 12.dp),
                                animationStarted = step.value == WelcomeOnboardingContent.STEP_OPTIONS_2
                            )
                        }

                        WelcomeOnboardingContent.STEP_LOCATION,
                        WelcomeOnboardingContent.STEP_LOCATION_ENCRYPTION,
                        null -> {
                            WelcomeOnboardingPageLocation(
                                modifier = Modifier.padding(top = 12.dp),
                                isEncryptionOn = step.value == WelcomeOnboardingContent.STEP_LOCATION_ENCRYPTION,
                                onNewRandomString = { vm.provideRandomString() }
                            )
                        }
                    }
                }
            )
        }
    )
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
