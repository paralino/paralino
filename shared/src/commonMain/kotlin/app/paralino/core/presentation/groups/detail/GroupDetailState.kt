package app.paralino.core.presentation.groups.detail

import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.utils.ConditionErrorType

data class GroupDetailScreenState(
    val id: String,
    val canEdit: Boolean,
    val loadingAction: Boolean,
    val name: String,
    val image: ImageModel,
    val sharingStatus: LocationSharingState?,
    val members: List<GroupMemberItemState>,
    val membersLimit: Int,
    val canShowMembersLimit: Boolean,
    val shouldShowPlanBanner: Boolean,
    val arePlacesOverLimit: Boolean?,
    val isGroupOverLimit: Boolean?,
    val errors: List<ConditionErrorType>
)

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

sealed class GroupDetailScreenEvent() {
    data class Error(val error: Throwable) : GroupDetailScreenEvent()
    object GroupDeleted : GroupDetailScreenEvent()
    object GroupNotFound : GroupDetailScreenEvent()
    object GroupMemberAdd : GroupDetailScreenEvent()
    data object OpenPaywall : GroupDetailScreenEvent()
    object GroupMemberDeleted : GroupDetailScreenEvent()
    object UserBlockActionCompleted : GroupDetailScreenEvent()
    object MemberLocationDeleted : GroupDetailScreenEvent()
}