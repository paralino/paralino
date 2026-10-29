package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.onboarding.AccountOnboardingContent
import app.paralino.core.presentation.onboarding.AccountOnboardingUiEvent
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.BaseOnboardingScrollableContent
import app.paralino.core.ui.components.BaseOnboardingHeaderContent
import app.paralino.core.ui.components.BaseOnboardingScreen
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountOnboardingScreenContent(
    coreUiState: CoreUiState,
    modifier: Modifier = Modifier,
    step: AccountOnboardingContent,
    onEvent: (AccountOnboardingUiEvent) -> Unit
) {
    BaseOnboardingScreen(
        modifier = modifier.fillMaxSize(),
        buttonText = step.buttonText?.let { stringResource(it) }
            ?: "",
        skipButtonText = step.skipButtonText?.let { stringResource(it) }
            ?: stringResource(MR.strings.button_skip),
        onSkip = if (step.skipButtonText != null) {
            { onEvent(AccountOnboardingUiEvent.SkipOnboarding) }
        } else {
            null
        },
        onNext = if (step.buttonText != null) {
            { onEvent(AccountOnboardingUiEvent.ConfirmOnboarding) }
        } else {
            null
        },
    ) {
        BaseOnboardingScrollableContent(
            order = step.titleKey,
            title = stringResource(step.title),
            info = step.infoText(),
            contentSizeRatio = step.contentSizeRatio,
            minHeaderSize = step.minHeaderSize,
            paddingValues = it,
            innerPaddingValues = remember { PaddingValues() },
            headerContent = {
                BaseOnboardingHeaderContent(
                    titleKey = step.titleKey,
                    titleTag = step.titleTag?.let { stringResource(it) },
                    content = {
                        when (step) {

                            AccountOnboardingContent.GroupOnboardingIntro -> {
                                AccountOnboardingGroupsIntro()
                            }

                            AccountOnboardingContent.GroupChoice -> {
                                AccountOnboardingGroupChoice(
                                    title = stringResource(step.title),
                                    info = step.infoText(),
                                    onJoinGroup = {
                                        onEvent(AccountOnboardingUiEvent.SelectJoinGroup)
                                    },
                                    onCreateGroup = {
                                        onEvent(AccountOnboardingUiEvent.SelectCreateGroup)
                                    }
                                )
                            }

                            is AccountOnboardingContent.ParalinoID -> {
                                AccountOnboardingParalinoID(
                                    step = step,
                                    title = stringResource(step.title),
                                    info = step.infoText(),
                                    onSharePersonalCode = {
                                        onEvent(AccountOnboardingUiEvent.SharePersonalCode)
                                    },
                                    onCopyPersonalCode = {
                                        onEvent(AccountOnboardingUiEvent.CopyPersonalCode)
                                    }
                                )
                            }

                            AccountOnboardingContent.GroupCreate -> {
                                AccountOnboardingGroupCreateIntro(
                                    title = stringResource(step.title)
                                )
                            }

                            AccountOnboardingContent.PushNotifications -> {
                                AccountOnboardingPushNotifications()
                            }

                            AccountOnboardingContent.PrivacyZonesIntro -> {
                                AccountOnboardingPrivacyZones()
                            }
                        }
                    }
                )
            }
        )
    }
}
