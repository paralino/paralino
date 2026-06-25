package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.paralino.core.MR
import app.paralino.core.presentation.onboarding.AccountOnboardingContent
import app.paralino.core.presentation.onboarding.AccountOnboardingUiEvent
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.BaseOnboardingScrollableContent
import app.paralino.core.ui.components.BaseOnboardingHeaderContent
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.settings.WelcomeOnboardingPageLocationHistory
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
                            is AccountOnboardingContent.ParalinoID -> {
                                AccountOnboardingParalinoID(
                                    step = step,
                                    onSharePersonalCode = {
                                        onEvent(AccountOnboardingUiEvent.SharePersonalCode)
                                    },
                                    onCopyPersonalCode = {
                                        onEvent(AccountOnboardingUiEvent.CopyPersonalCode)
                                    }
                                )
                            }

                            is AccountOnboardingContent.AccountOnboardingStart -> {
                                AccountOnboardingStartPage(
                                    isGroupChecked = step.isGroupChecked,
                                    isPersonalSpaceChecked = step.isPersonalSpaceChecked,
                                    onGroupModeClick = {
                                        onEvent(AccountOnboardingUiEvent.GoWithGroupMode)
                                    },
                                    onPersonalSpaceModeClick = {
                                        onEvent(AccountOnboardingUiEvent.GoWithPersonalSpaceMode)
                                    },
                                )
                            }

                            AccountOnboardingContent.PushNotifications -> {
                                AccountOnboardingPushNotifications()
                            }

                            AccountOnboardingContent.GroupCreate -> {
                                AccountOnboardingGroupCreate()
                            }

                            AccountOnboardingContent.PersonalSpaceCurrentUsersIntro -> {
                                AccountOnboardingPersonalSpace()
                            }

                            AccountOnboardingContent.PersonalSpaceNewUsersIntro -> {
                                AccountOnboardingPersonalSpace()
                            }

                            is AccountOnboardingContent.LocationHistoryIntro -> {
                                WelcomeOnboardingPageLocationHistory()
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
