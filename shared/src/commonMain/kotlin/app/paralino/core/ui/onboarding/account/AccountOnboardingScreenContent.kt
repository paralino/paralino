package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.onboarding.BaseOnboardingContent
import app.paralino.core.ui.onboarding.BaseOnboardingHeaderContent
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
import app.paralino.core.ui.onboarding.welcome.WelcomeOnboardingPageLocationHistory
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import org.koin.core.qualifier.qualifier

@Composable
fun AccountOnboardingScreenContent(
    coreUiState: CoreUiState,
    modifier: Modifier = Modifier,
    step: AccountOnboardingContent,
    onEvent: (AccountOnboardingUiEvent) -> Unit
) {
    BaseOnboardingScreen(
        modifier = modifier.fillMaxSize(),
        buttonText = stringResource(step.buttonText),
        skipButtonText = step.skipButtonText?.let { stringResource(it) }
            ?: stringResource(MR.strings.button_skip),
        onSkip = if (step.skipButtonText != null) {
            { onEvent(AccountOnboardingUiEvent.SkipOnboarding) }
        } else {
            null
        },
        onNext = { onEvent(AccountOnboardingUiEvent.ConfirmOnboarding) },
    ) {
        if (step is AccountOnboardingContent.SilverPlan) {
            AccountOnboardingSilverPlanBackground()
        }

        BaseOnboardingContent(
            order = step.titleKey,
            title = stringResource(step.title),
            info = step.infoText(),
            contentSizeRatio = step.contentSizeRatio,
            minHeaderSize = step.minHeaderSize,
            paddingValues = it,
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

                            AccountOnboardingContent.PushNotifications -> {
                                AccountOnboardingPushNotifications()
                            }

                            AccountOnboardingContent.GroupCreate -> {
                                AccountOnboardingGroupCreate()
                            }

                            is AccountOnboardingContent.SilverPlan -> {
                                AccountOnboardingSilverPlan()
                            }

                            AccountOnboardingContent.PersonalSpaceIntro -> {
                                AccountOnboardingPersonalSpace()
                            }

                            is AccountOnboardingContent.LocationHistoryIntro -> {
                                WelcomeOnboardingPageLocationHistory()
                            }
                        }
                    }
                )
            }
        )
    }
}

sealed class AccountOnboardingContent(
    val parentKey: Int,
    val titleKey: Int,
    val titleTag: StringResource?,
    val title: StringResource,
    val buttonText: StringResource,
    val skipButtonText: StringResource?,
    val contentSizeRatio: Float = 0.3f,
    val minHeaderSize: Dp = 300.dp
) {
    @Composable
    abstract fun infoText(): String?

    data class ParalinoID(
        val personalCode: PersonalCodeScreenCodeData?,
        val didShareCode: Boolean
    ) : AccountOnboardingContent(
        parentKey = 100,
        titleKey = 100,
        titleTag = MR.strings.onboarding_step_paralinoID_title_tag,
        title = MR.strings.onboarding_step_paralinoID_title,
        buttonText = if (didShareCode) {
            MR.strings.button_next
        } else {
            MR.strings.onboarding_step_paralinoID_button_next
        },
        skipButtonText = if (didShareCode) {
            null
        } else {
            MR.strings.button_skip
        },
        minHeaderSize = Dp.Unspecified
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_paralinoID_info)
        }
    }

    data object GroupCreate : AccountOnboardingContent(
        parentKey = 200,
        titleKey = 200,
        titleTag = MR.strings.onboarding_step_groupCreate_title_tag,
        title = MR.strings.onboarding_step_groupCreate_title,
        buttonText = MR.strings.onboarding_step_groupCreate_button_next,
        skipButtonText = MR.strings.button_notNow
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_groupCreate_info)
        }
    }

    data class SilverPlan(
        val planConfig: PlanConfig
    ) : AccountOnboardingContent(
        parentKey = 300,
        titleKey = 300,
        titleTag = MR.strings.plans_gift_screen_title,
        title = MR.strings.plans_gift_screen_benefits_title,
        buttonText = MR.strings.button_continue,
        skipButtonText = null,
        contentSizeRatio = 0.4f
    ) {
        @Composable
        override fun infoText(): String {
            return remember(planConfig) {
                planConfig.planBenefits().joinToString(separator = "\n") {
                    it.content
                }
            }
        }
    }

    data object PushNotifications : AccountOnboardingContent(
        parentKey = 400,
        titleKey = 400,
        titleTag = MR.strings.onboarding_step_notifications_title_tag,
        title = MR.strings.onboarding_step_notifications_title,
        buttonText = MR.strings.onboarding_step_notifications_button_next,
        skipButtonText = MR.strings.button_notNow
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_notifications_info)
        }
    }

    data object PersonalSpaceIntro : AccountOnboardingContent(
        parentKey = 500,
        titleKey = 500,
        titleTag = MR.strings.personalSpace_spaceTitle,
        title = MR.strings.onboarding_step_personalSpace_title,
        buttonText = MR.strings.button_continue,
        skipButtonText = null
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_personalSpace_info)
        }
    }

    data class LocationHistoryIntro(
        val isSharingActive: Boolean
    ) : AccountOnboardingContent(
        parentKey = 600,
        titleKey = 600,
        titleTag = MR.strings.setting_location_history_section_title,
        title = if (isSharingActive) {
            MR.strings.onboarding_step_locationHistoryIntro_title_active
        } else {
            MR.strings.onboarding_step_locationHistoryIntro_title_inactive
        },
        buttonText = if (isSharingActive) {
            MR.strings.onboarding_step_locationHistoryIntro_button_next
        } else {
            MR.strings.button_continue
        },
        skipButtonText = if (isSharingActive) {
            MR.strings.button_notNow
        } else {
            null
        }
    ) {
        @Composable
        override fun infoText(): String {
            return if (isSharingActive) {
                stringResource(MR.strings.onboarding_step_locationHistoryIntro_info_active)
            } else {
                stringResource(MR.strings.onboarding_step_locationHistory_info)
            }
        }
    }
}

sealed interface AccountOnboardingUiEvent {
    data object ConfirmOnboarding : AccountOnboardingUiEvent
    data object SkipOnboarding : AccountOnboardingUiEvent
    data object CopyPersonalCode : AccountOnboardingUiEvent
    data object SharePersonalCode : AccountOnboardingUiEvent
}