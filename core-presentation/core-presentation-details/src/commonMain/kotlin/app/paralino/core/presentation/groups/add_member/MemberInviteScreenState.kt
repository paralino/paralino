package app.paralino.core.presentation.groups.add_member

import app.paralino.core.domain.user.PremiumStatusLimitState
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.presentation.common.GroupItemState
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.utils.Emojis

data class MemberInviteScreenState(
    val code: String = "",
    val hasInputError: Boolean = false,
    val actionLoading: Boolean = false,
    val group: GroupItemState? = null,
    val members: List<MemberModel> = emptyList(),
    val premiumStatus: PremiumStatusLimitState? = null,
    val loadedState: MemberInviteScreenLoadedState? = null,
    val alertDialog: MainAlertDialog? = null
) {
    val isLoaded: Boolean = loadedState != null
    val shouldUpgradePlan = isLoaded && premiumStatus is PremiumStatusLimitState.PlanLimitReached
    val isNextButtonEnabled = !isLoaded || group == null || (premiumStatus != null && premiumStatus is PremiumStatusLimitState.WithinLimits)
}

data class MemberInviteScreenLoadedState constructor(
    val codeReadData: UserPersonalCodeData,
    val canChangeGroup: Boolean,
) {
    val code: String get() = codeReadData.keyData.formattedCode
    val userId: String get() = codeReadData.userData.ownerUserId
    val userName: String get() = codeReadData.userData.ownerName
    val userImage: ImageModel
        get() = codeReadData.userData.ownerImage ?: ImageModel.emoji(Emojis.robot)
}

sealed interface MemberInviteScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : MemberInviteScreenUiEvent
}

sealed class MemberInviteScreenEvent() {
    object MemberAdded : MemberInviteScreenEvent()
    data class ShowGroupPicker(val sheetState: BottomSheetPickerType.GroupPickerBottomSheet) : MemberInviteScreenEvent()
    data class OpenPaywall(val type: BillingScreenSourceType) : MemberInviteScreenEvent()
}
