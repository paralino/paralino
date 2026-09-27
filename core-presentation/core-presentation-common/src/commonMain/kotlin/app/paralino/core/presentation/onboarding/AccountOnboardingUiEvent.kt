package app.paralino.core.presentation.onboarding

sealed interface AccountOnboardingUiEvent {
    data object ConfirmOnboarding : AccountOnboardingUiEvent
    data object SkipOnboarding : AccountOnboardingUiEvent
    data object CopyPersonalCode : AccountOnboardingUiEvent
    data object SharePersonalCode : AccountOnboardingUiEvent
}