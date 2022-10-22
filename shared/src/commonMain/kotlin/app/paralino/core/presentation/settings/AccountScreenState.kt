package app.paralino.core.presentation.settings

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.ConditionErrorType

data class AccountScreenState constructor(
    val userState: UserState,
    val logoutState: Payload<Unit>? = null,
    val deleteAccountState: Payload<Unit>? = null,
) {
    val loadingAction get() = logoutState is Payload.Loading || deleteAccountState is Payload.Loading
}

data class UserState constructor(
    val name: String,
    val userId: String,
    val email: String,
    val image: ImageModel?,
    val isGuestUser: Boolean,
    val errorItem: ConditionErrorType?,
)

sealed class AccountScreenEvent() {
    data class Error(val error: Throwable): AccountScreenEvent()
    object EmailVerificationSent: AccountScreenEvent()
}