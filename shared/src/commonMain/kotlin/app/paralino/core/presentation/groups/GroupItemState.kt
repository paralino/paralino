package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.models.user.UserMe
import app.paralino.core.utils.Emojis

data class GroupItemState constructor(
    val id: GroupOrSpaceId,
    val name: String,
    val image: ImageModel,
    val membersCount: Int?,
    val devicesCount: Int?,
    val placesCount: Int,
    val isPersonalSpace: Boolean,
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
        id = group.group.id.asGroupOrSpaceId(),
        name = group.name,
        image = group.image,
        membersCount = group.memberCount,
        placesCount = group.placesCount,
        devicesCount = null,
        isPersonalSpace = false,
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

    constructor(
        space: UserPersonalSpaceModel,
        config: ConfigData? = null,
        uiType: GroupUiType? = null,
        canEdit: Boolean = false,
        isSelected: Boolean = false
    ) : this(
        id = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
        name = "Personal space",
        image = space.user.data?.userImage ?: ImageModel.emoji(Emojis.robot),
        membersCount = null,
        placesCount = space.placesCount,
        devicesCount = space.activeDeviceCount,
        isPersonalSpace = true,
        uiType = uiType ?: GroupUiType.INACTIVE,
        canEdit = true,
        isCurrentUserOwner = true,
        isSelected = isSelected,
        isLarge = true
    )

    enum class GroupUiType {
        OVER_LIMITS, UNVERIFIED, ACTIVE, INACTIVE
    }
}