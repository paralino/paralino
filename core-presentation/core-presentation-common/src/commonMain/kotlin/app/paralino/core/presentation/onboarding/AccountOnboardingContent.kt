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

    data object GroupOnboardingIntro : AccountOnboardingContent(
        parentKey = 100,
        titleKey = 100,
        titleTag = MR.strings.onboarding_step_groupIntro_title_tag,
        title = MR.strings.onboarding_step_groupIntro_title,
        buttonText = MR.strings.button_next,
        skipButtonText = null,
        minHeaderSize = 400.dp
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_groupIntro_info)
        }
    }

    data object GroupChoice : AccountOnboardingContent(
        parentKey = 150,
        titleKey = 150,
        titleTag = MR.strings.onboarding_step_groupIntro_title_tag,
        title = MR.strings.onboarding_step_groupChoice_title,
        buttonText = null,
        skipButtonText = MR.strings.onboarding_step_groupChoice_button_later,
        // title and info are part of the header content, above the choices
        contentSizeRatio = 0f,
        minHeaderSize = Dp.Unspecified
    ) {
        @Composable
        override fun infoText(): String {
            return stringResource(MR.strings.onboarding_step_groupChoice_info)
        }
    }

    data class ParalinoID(
        val personalCode: PersonalCodeScreenCodeData?,
        val didShareCode: Boolean
    ) : AccountOnboardingContent(
        parentKey = 200,
        titleKey = 200,
        titleTag = MR.strings.onboarding_step_paralinoID_title_tag_join,
        title = if (didShareCode) {
            MR.strings.onboarding_step_paralinoID_title_waiting
        } else {
            MR.strings.invite_manage_description
        },
        buttonText = if (didShareCode) {
            MR.strings.onboarding_step_paralinoID_button_explore
        } else {
            MR.strings.onboarding_step_paralinoID_button_next
        },
        skipButtonText = MR.strings.onboarding_step_paralinoID_button_createInstead,
        // title and info are part of the header content
        contentSizeRatio = 0f,
        minHeaderSize = Dp.Unspecified
    ) {
        @Composable
        override fun infoText(): String? {
            return if (didShareCode) {
                stringResource(MR.strings.onboarding_step_paralinoID_info_waiting)
            } else {
                null
            }
        }
    }

    data object GroupCreate : AccountOnboardingContent(
        parentKey = 300,
        titleKey = 300,
        titleTag = MR.strings.onboarding_step_groupCreate_admin_title_tag,
        title = MR.strings.onboarding_step_groupCreate_admin_title,
        buttonText = MR.strings.button_continue,
        skipButtonText = MR.strings.onboarding_step_groupCreate_button_joinInstead,
        // title and admin info rows are part of the header content
        contentSizeRatio = 0f,
        minHeaderSize = Dp.Unspecified
    ) {
        @Composable
        override fun infoText(): String? = null
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