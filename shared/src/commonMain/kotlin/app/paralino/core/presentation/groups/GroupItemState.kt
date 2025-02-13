package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ConfigData
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
        config: ConfigData? = null,
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
        uiType = uiType ?: when {
            !group.isVerified -> GroupUiType.UNVERIFIED
            config?.let { group.isGroupOverLimits(it) } == true -> GroupUiType.OVER_LIMITS
            else -> GroupUiType.INACTIVE
        },
        canEdit = canEdit,
        isCurrentUserOwner = group.group.isCurrentUserOwner,
        isSelected = isSelected,
        isLarge = isLarge
    )

    enum class GroupUiType {
        OVER_LIMITS, UNVERIFIED, ACTIVE, INACTIVE
    }
}