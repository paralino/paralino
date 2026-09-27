package app.paralino.core.presentation.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.paralinoid.PersonalCodeScreenCodeData
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

sealed class AccountOnboardingContent(
    val parentKey: Int,
    val titleKey: Int,
    val titleTag: StringResource?,
    val title: StringResource,
    val buttonText: StringResource?,
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
        title = if (didShareCode) {
            MR.strings.onboarding_step_paralinoID_title_shared
        } else {
            MR.strings.onboarding_step_paralinoID_title_intro
        },
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
            return if (didShareCode) {
                ""
            } else {
                stringResource(MR.strings.onboarding_step_paralinoID_info)
            }
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

    data object PrivacyZonesIntro : AccountOnboardingContent(
        parentKey = 650,
        titleKey = 650,
        titleTag = MR.strings.onboarding_step_privacyZonesIntro_title_tag,
        title = MR.strings.onboarding_step_privacyZonesIntro_title,
        buttonText = MR.strings.button_ok,
        skipButtonText = null
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_privacyZonesIntro_info)
        }
    }
}