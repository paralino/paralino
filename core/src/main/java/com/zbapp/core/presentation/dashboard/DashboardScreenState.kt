package com.zbapp.core.presentation.dashboard

import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.utils.ConditionErrorType

data class DashboardScreenState(
    val activeGroups: List<GroupItemData>,
    val inactiveGroups: List<GroupItemData>,
    val errors: List<ConditionErrorType>,
) {

    val isWithoutGroups get() = activeGroups.isEmpty() && inactiveGroups.isEmpty()
}

data class GroupItemData constructor(
    val id: String,
    val name: String,
    val image: ImageModel,
    val userCount: Int,
    val uiType: GroupUiType = GroupUiType.INACTIVE,
) {

    constructor(group: UserGroup, uiType: GroupUiType = GroupUiType.PICKER) : this(
        group.id, group.name, group.image, group.memberCount, uiType
    )

    enum class GroupUiType {
        ACTIVE, INACTIVE, PICKER
    }
}

sealed class DashboardScreenEvent() {
    data class Error(val error: Throwable) : DashboardScreenEvent()
}