package app.paralino.core.models.base

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.UserPersonalSpaceModel

sealed interface GroupOrUserSpace {
    val id: String
    data class Group(val group: UserGroupWithMetadata): GroupOrUserSpace {
        override val id: String = group.id
    }
    data class PersonalSpace(val space: UserPersonalSpaceModel): GroupOrUserSpace {
        override val id: String = UserGroup.groupPersonalDevicesId
    }
}

sealed interface GroupWithMembersOrUserSpace {
    val id: GroupOrSpaceId
    data class Group(val group: UserGroupWithMembers): GroupWithMembersOrUserSpace {
        override val id: GroupOrSpaceId = group.id.asGroupOrSpaceId()
    }
    data class PersonalSpace(val space: UserPersonalSpaceModel): GroupWithMembersOrUserSpace {
        override val id: GroupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()
    }
}