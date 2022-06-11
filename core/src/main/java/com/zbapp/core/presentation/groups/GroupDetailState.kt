package com.zbapp.core.presentation.groups

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.utils.ConditionErrorType

data class GroupDetailScreenState(
    val id: String,
    val canEdit: Boolean,
    val loadingAction: Boolean,
    val name: String,
    val image: ImageModel,
    val sharingStatus: LocationSharingContract?,
    val members: List<GroupMemberItemState>,
    val errors: List<ConditionErrorType>
) {
}

sealed class GroupDetailScreenEvent() {
    data class Error(val error: Throwable) : GroupDetailScreenEvent()
    object GroupDeleted : GroupDetailScreenEvent()
    object GroupNotFound : GroupDetailScreenEvent()
    object GroupMemberDeleted : GroupDetailScreenEvent()
}