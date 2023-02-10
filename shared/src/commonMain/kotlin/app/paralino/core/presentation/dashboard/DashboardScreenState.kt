package app.paralino.core.presentation.dashboard

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup
import app.paralino.core.utils.ConditionErrorType

data class DashboardScreenState constructor(
    val email: String,
    val groups: List<GroupItemData>,
    val errors: List<ConditionErrorType>,
) {

    val isWithoutGroups get() = groups.isEmpty()
}

data class GroupItemData constructor(
    val id: String,
    val name: String,
    val image: ImageModel,
    val userCount: Int,
    val uiType: GroupUiType = GroupUiType.INACTIVE,
) {

    constructor(group: UserGroup, uiType: GroupUiType? = null) : this(
        group.id, group.name, group.image, group.memberCount, uiType ?: if(!group.isVerified) GroupUiType.UNVERIFIED else GroupUiType.NONE
    )

    enum class GroupUiType {
        UNVERIFIED, ACTIVE, INACTIVE, NONE
    }
}

sealed class DashboardScreenEvent() {
    data class Error(val error: Throwable) : DashboardScreenEvent()
    object EmailVerificationSent : DashboardScreenEvent()
}