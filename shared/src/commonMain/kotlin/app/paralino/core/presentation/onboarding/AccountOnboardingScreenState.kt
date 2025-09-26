package app.paralino.core.presentation.onboarding

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.invite.manage.PersonalCodeScreenCodeData
import app.paralino.core.ui.onboarding.account.AccountOnboardingContent
import app.paralino.core.ui.onboarding.account.AccountOnboardingUiEvent
import app.paralino.core.ui.settings.billing.BillingScreenSourceType

data class AccountOnboardingScreenState constructor(
    val userImage: ImageModel,
    val personalCode: PersonalCodeScreenCodeData?,
    val onboardingStep: AccountOnboardingContent?,
    val groupCount: Int,
    val fullScreenLoading: Boolean,
) {
    val canDisplayMap: Boolean = onboardingStep == null
}

sealed interface AccountOnboardingScreenEvent {
    data object AskNotificationPermission: AccountOnboardingScreenEvent
    data object OpenPersonalCodes: AccountOnboardingScreenEvent
    data object OpenGroupCreate: AccountOnboardingScreenEvent
    data class OpenPaywall(val type: BillingScreenSourceType): AccountOnboardingScreenEvent
    data class SharePersonalCode(val code: String): AccountOnboardingScreenEvent
}

sealed class AccountOnboardingScreenUiEvent() {
    data class OnboardingStep(val event: AccountOnboardingUiEvent): AccountOnboardingScreenUiEvent()
}