package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.utils.ConditionErrorType

data class GroupDetailScreenState(
    val id: String,
    val canEdit: Boolean,
    val loadingAction: Boolean,
    val name: String,
    val image: ImageModel,
    val sharingStatus: LocationSharingContract?,
    val members: List<GroupMemberItemState>,
    val memberLimit: Int,
    val errors: List<ConditionErrorType>
) {
}

sealed class GroupDetailScreenEvent() {
    data class Error(val error: Throwable) : GroupDetailScreenEvent()
    object GroupDeleted : GroupDetailScreenEvent()
    object GroupNotFound : GroupDetailScreenEvent()
    object GroupMemberAdd : GroupDetailScreenEvent()
    data class GroupMembersLimitReached(val limit: Int) : GroupDetailScreenEvent()
    object GroupMemberDeleted : GroupDetailScreenEvent()
    object MemberLocationDeleted : GroupDetailScreenEvent()
}