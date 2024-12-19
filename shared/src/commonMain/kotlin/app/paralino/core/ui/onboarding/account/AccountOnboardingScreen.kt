package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.paralino.core.MR
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.onboarding.BaseOnboardingContent
import app.paralino.core.ui.onboarding.BaseOnboardingHeaderContent
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun AccountOnboardingScreen(
    coreUiState: CoreUiState,
    paddingValues: PaddingValues,
    step: AccountOnboardingContent,
    onEvent: (AccountOnboardingUiEvent) -> Unit
) {
    BaseOnboardingScreen(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
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
    val contentSizeRatio: Float = 0.3f
) {
    @Composable
    abstract fun infoText(): String?

    data class ParalinoID(
        val personalCode: PersonalCodeScreenCodeData?
    ) : AccountOnboardingContent(
        parentKey = 100,
        titleKey = 100,
        titleTag = MR.strings.onboarding_step_paralinoID_title_tag,
        title = MR.strings.onboarding_step_paralinoID_title,
        buttonText = MR.strings.onboarding_step_paralinoID_button_next,
        skipButtonText = MR.strings.button_notNow,
        contentSizeRatio = 0.2f
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
        skipButtonText = MR.strings.button_skip
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_notifications_info)
        }
    }
}

sealed interface AccountOnboardingUiEvent {
    data object ConfirmOnboarding : AccountOnboardingUiEvent
    data object SkipOnboarding : AccountOnboardingUiEvent
    data object CopyPersonalCode : AccountOnboardingUiEvent
    data object SharePersonalCode : AccountOnboardingUiEvent
}