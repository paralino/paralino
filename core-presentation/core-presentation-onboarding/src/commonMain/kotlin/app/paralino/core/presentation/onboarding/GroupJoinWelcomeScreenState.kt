package app.paralino.core.presentation.onboarding

import androidx.compose.runtime.Composable
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

data class GroupJoinWelcomeScreenState(
    val isLoading: Boolean,
    val groupName: String?,
    val groupImage: ImageModel?,
    val members: List<GroupJoinWelcomePreviewChipData> = emptyList(),
    val places: List<GroupJoinWelcomePreviewChipData> = emptyList(),
    val isSharingActive: Boolean = false,
    val step: GroupJoinWelcomeStepContent = GroupJoinWelcomeStepContent.WelcomeLanding
)

data class GroupJoinWelcomePreviewChipData(
    val id: String,
    val name: String,
    val image: ImageModel
)

sealed class GroupJoinWelcomeStepContent(
    val parentKey: Int,
    val titleKey: Int,
    val info: StringResource?,
    val buttonText: StringResource
) {
    @Composable
    abstract fun title(): String

    data object WelcomeLanding : GroupJoinWelcomeStepContent(
        parentKey = 100,
        titleKey = 100,
        info = null,
        buttonText = MR.strings.onboarding_step_groupJoinWelcome_button_start
    ) {
        @Composable
        override fun title(): String {
            return stringResource(MR.strings.group_member_current_add_notification_default)
        }
    }

    data object ShareLocation : GroupJoinWelcomeStepContent(
        parentKey = 200,
        titleKey = 200,
        info = MR.strings.onboarding_step_groupJoinWelcome_share_info,
        buttonText = MR.strings.button_continue
    ) {
        @Composable
        override fun title(): String {
            return stringResource(MR.strings.onboarding_step_groupJoinWelcome_share_title)
        }
    }

    data object Done : GroupJoinWelcomeStepContent(
        parentKey = 200,
        titleKey = 300,
        info = MR.strings.onboarding_step_groupJoinWelcome_done_info,
        buttonText = MR.strings.onboarding_step_groupCreate_button_next
    ) {
        @Composable
        override fun title(): String {
            return stringResource(MR.strings.onboarding_step_groupJoinWelcome_done_title)
        }
    }
}

sealed interface GroupJoinWelcomeScreenEvent {
    data object OpenSharing : GroupJoinWelcomeScreenEvent
    data object OpenMap : GroupJoinWelcomeScreenEvent
}

sealed interface GroupJoinWelcomeScreenUiEvent {
    data object Confirm : GroupJoinWelcomeScreenUiEvent
}
