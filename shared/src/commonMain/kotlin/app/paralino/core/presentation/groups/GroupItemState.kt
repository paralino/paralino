package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroupWithMetadata

data class GroupItemState constructor(
    val id: String,
    val name: String,
    val image: ImageModel,
    val userCount: Int,
    val placesCount: Int,
    val uiType: GroupUiType = GroupUiType.INACTIVE,
    val canEdit: Boolean,
    val isCurrentUserOwner: Boolean,
    val isSelected: Boolean,
    val isLarge: Boolean,
) {

    constructor(
        group: UserGroupWithMetadata,
        uiType: GroupUiType? = null,
        canEdit: Boolean = false,
        isSelected: Boolean = false,
        isLarge: Boolean = true
    ) : this(
        id = group.group.id,
        name = group.name,
        image = group.image,
        userCount = group.memberCount,
        placesCount = group.placesCount,
        uiType = uiType ?: if (!group.isVerified) GroupUiType.UNVERIFIED else GroupUiType.INACTIVE,
        canEdit = canEdit,
        isCurrentUserOwner = group.group.isCurrentUserOwner,
        isSelected = isSelected,
        isLarge = isLarge
    )

    enum class GroupUiType {
        UNVERIFIED, ACTIVE, INACTIVE
    }
}