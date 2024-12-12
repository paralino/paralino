package app.paralino.android.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import app.paralino.core.presentation.onboarding.WelcomeOnboardingViewModel
import app.paralino.core.ui.onboarding.welcome.WelcomeOnboardingContent
import app.paralino.core.ui.onboarding.welcome.WelcomeOnboardingScreenCore
import org.koin.androidx.compose.koinViewModel

@Composable
fun WelcomeOnboardingScreen(
    onNext: () -> Unit,
    vm: WelcomeOnboardingViewModel = koinViewModel()
) {
    val step = rememberSaveable { mutableStateOf(WelcomeOnboardingContent.STEP_LOCATION) }
    BackHandler(step.value.ordinal > 0) {
        val previous = step.value.previous()
        if (previous != null) {
            step.value = previous
        }
    }
    WelcomeOnboardingScreenCore(
        vm = vm,
        step = step,
        onNext = onNext
    )
}