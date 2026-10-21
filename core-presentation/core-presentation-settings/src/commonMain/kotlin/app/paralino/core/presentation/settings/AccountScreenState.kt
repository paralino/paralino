package app.paralino.core.presentation.settings

import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType

data class AccountScreenState constructor(
    val userState: UserState,
    val canUseEmail: Boolean,
    val logoutState: Payload<Unit>? = null,
    val deleteAccountState: Payload<Unit>? = null,
    val versionName: String,
    val alertDialog: MainAlertDialog? = null,
    val userPlan: PlanConfig,
    val showUpgradeBanner: Boolean,
    val appReviewUrl: String,
    val isAppReviewExpanded: Boolean,
    val isAppReviewVisible: Boolean,
    val errors: List<ConditionErrorType>,
    val email: String?,
    val userId: String,
    val billingId: String,
    val deviceId: String,
    val diagnosticsId: String,
    val pushToken: String?,
    val memberSinceDateFormatted: String,
) {
    val loadingAction get() = logoutState is Payload.Loading || deleteAccountState is Payload.Loading
}

data class UserState constructor(
    val name: String,
    val image: ImageModel?,
    val email: String,
    val personalCode: String?,
    val isGuestUser: Boolean
)

sealed class AccountScreenEvent() {
    object EmailVerificationSent: AccountScreenEvent()
    object OpenAppReview: AccountScreenEvent()
}

sealed class AccountScreenUiEvent() {
    data object Logout: AccountScreenUiEvent()
    data object DeleteAccount: AccountScreenUiEvent()
    data object SendEmailVerification: AccountScreenUiEvent()
    data object TryToOpenAppReview: AccountScreenUiEvent()
    data object OpenAppReview: AccountScreenUiEvent()
    data object CloseAppReview: AccountScreenUiEvent()
    data object TriggerAppUpdate: AccountScreenUiEvent()
    data class Copy(val content: String): AccountScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : AccountScreenUiEvent()
}