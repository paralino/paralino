package app.paralino.core.presentation.groups.edit

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.onboarding.account.AccountOnboardingContent
import app.paralino.core.ui.onboarding.account.AccountOnboardingUiEvent
import app.paralino.core.utils.ConditionErrorType

data class GroupInitScreenState constructor(
    val userImage: ImageModel,
    val personalCode: PersonalCodeScreenCodeData?,
    val errors: List<ConditionErrorType> = emptyList(),
    val alertDialog: MainAlertDialog?,
    val onboardingStep: AccountOnboardingContent?,
    val groupCount: Int
)

sealed interface GroupInitScreenEvent {
    data object AskNotificationPermission: GroupInitScreenEvent
    data object OpenPersonalCodes: GroupInitScreenEvent
    data object OpenGroupCreate: GroupInitScreenEvent
    data class SharePersonalCode(val code: String): GroupInitScreenEvent
}

sealed class GroupInitScreenUiEvent() {
    data object CopyPersonalCode: GroupInitScreenUiEvent()
    data object HideDialog: GroupInitScreenUiEvent()
    data object SendEmailVerification: GroupInitScreenUiEvent()
    data object ConfirmSendEmailVerification: GroupInitScreenUiEvent()
    data class OnboardingStep(val event: AccountOnboardingUiEvent): GroupInitScreenUiEvent()
}