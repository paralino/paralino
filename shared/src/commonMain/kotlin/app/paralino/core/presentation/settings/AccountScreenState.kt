package app.paralino.core.presentation.settings

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType

data class AccountScreenState constructor(
    val userState: UserState,
    val canUseEmail: Boolean,
    val logoutState: Payload<Unit>? = null,
    val deleteAccountState: Payload<Unit>? = null,
    val versionName: String,
    val alertDialog: MainAlertDialog? = null,
    val boostTime: String? = null,
    val userPlan: PlanConfig
) {
    val loadingAction get() = logoutState is Payload.Loading || deleteAccountState is Payload.Loading
}

data class UserState constructor(
    val name: String,
    val image: ImageModel?,
    val email: String,
    val personalCode: String?,
    val isGuestUser: Boolean,
    val errorItem: ConditionErrorType?,
)

sealed class AccountScreenEvent() {
    object EmailVerificationSent: AccountScreenEvent()
}

sealed class AccountScreenUiEvent() {
    data object Logout: AccountScreenUiEvent()
    data object DeleteAccount: AccountScreenUiEvent()
    data object SendEmailVerification: AccountScreenUiEvent()
    data object ToggleLocationBoost: AccountScreenUiEvent()
    data class Copy(val content: String): AccountScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : AccountScreenUiEvent()
}