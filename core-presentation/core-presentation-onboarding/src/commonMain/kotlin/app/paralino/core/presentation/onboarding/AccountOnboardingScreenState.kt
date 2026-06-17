package app.paralino.core.presentation.onboarding

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.paralinoid.PersonalCodeScreenCodeData

data class AccountOnboardingScreenState constructor(
    val userImage: ImageModel,
    val personalCode: PersonalCodeScreenCodeData?,
    val onboardingStep: AccountOnboardingContent?,
    val isGroupModeChecked: Boolean,
    val isPersonalSpaceModeChecked: Boolean,
    val didShareParalinoID: Boolean,
    val groupCount: Int,
    val fullScreenLoading: Boolean,
) {
    val canDisplayMap: Boolean = onboardingStep == null
}

sealed interface AccountOnboardingScreenEvent {
    data object AskNotificationPermission: AccountOnboardingScreenEvent
    data object OpenPersonalCodes: AccountOnboardingScreenEvent
    data object OpenGroupCreate: AccountOnboardingScreenEvent
    data class OpenSharingStart(val groupId: GroupOrSpaceId): AccountOnboardingScreenEvent
    data class OpenPaywall(val type: BillingScreenSourceType): AccountOnboardingScreenEvent
    data class SharePersonalCode(val code: String): AccountOnboardingScreenEvent
}

sealed class AccountOnboardingScreenUiEvent() {
    data class OnboardingStep(val event: AccountOnboardingUiEvent): AccountOnboardingScreenUiEvent()
}