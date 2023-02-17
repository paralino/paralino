package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup

data class GroupItemState constructor(
    val id: String,
    val name: String,
    val image: ImageModel,
    val userCount: Int,
    val uiType: GroupUiType = GroupUiType.INACTIVE,
) {

    constructor(group: UserGroup, uiType: GroupUiType? = null) : this(
        group.id, group.name, group.image, group.memberCount, uiType ?: if(!group.isVerified) GroupUiType.UNVERIFIED else GroupUiType.INACTIVE
    )

    enum class GroupUiType {
        UNVERIFIED, ACTIVE, INACTIVE
    }
}