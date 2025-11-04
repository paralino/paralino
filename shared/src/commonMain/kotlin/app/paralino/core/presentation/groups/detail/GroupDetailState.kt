package app.paralino.core.presentation.groups.detail

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType

data class GroupDetailScreenState(
    val groupId: String,
    val canEdit: Boolean,
    val loadingAction: Boolean,
    val name: String,
    val image: ImageModel,
    val sharingStatus: LocationSharingState?,
    val members: List<GroupMemberItemState>,
    val focusedMemberId: String?,
    val membersLimit: Int,
    val canShowMembersLimit: Boolean,
    val shouldShowPlanBanner: Boolean,
    val arePlacesOverLimit: Boolean?,
    val isGroupOverLimit: Boolean?,
    val errors: List<ConditionErrorType>,
    val alertDialog: MainAlertDialog?
) {
    val isMemberFocused: Boolean = !focusedMemberId.isNullOrBlank()
    val visibleMembers: List<GroupMemberItemState> = members.filter {
        if (isMemberFocused) {
            it.memberState.memberId == focusedMemberId
        } else {
            true
        }
    }
}

data class LocationSharingState(
    val endTime: String?,
    val shareBattery: Boolean,
    val shareSpeed: Boolean,
    val shareElevation: Boolean,
    val shareNetwork: Boolean,
    val shareDeviceStatus: Boolean,
    val saveToHistory: Boolean,
    val approximateLocation: String?
)

sealed interface GroupDetailScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : GroupDetailScreenUiEvent
    data class OnMemberItemEvent(val event: GroupMemberItemEvent) : GroupDetailScreenUiEvent
    data object OnWholeContentClick : GroupDetailScreenUiEvent
    data object OnAddNewMember : GroupDetailScreenUiEvent
    data object StopSharing : GroupDetailScreenUiEvent
    data object OpenMap : GroupDetailScreenUiEvent
}

sealed class GroupDetailScreenEvent() {
    object GroupDeleted : GroupDetailScreenEvent()
    object GroupMemberAdd : GroupDetailScreenEvent()
    data class OpenRawMemberData(val memberId: String) : GroupDetailScreenEvent()
    data object OpenPaywall : GroupDetailScreenEvent()
    data object OpenMap : GroupDetailScreenEvent()
    object GroupMemberDeleted : GroupDetailScreenEvent()
    object UserBlockActionCompleted : GroupDetailScreenEvent()
    object MemberLocationDeleted : GroupDetailScreenEvent()
}