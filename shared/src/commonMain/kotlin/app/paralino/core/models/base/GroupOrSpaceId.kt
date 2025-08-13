package app.paralino.core.models.base

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.UserPersonalSpaceModel
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

//typealias GroupOrSpaceId = String
@JvmInline
@Serializable
value class GroupOrSpaceId(val id: String) {
    val isPersonalSpace: Boolean get() = id == UserGroup.groupPersonalDevicesId
    val isPublicSharing: Boolean get() = id == UserGroup.groupPublicSharingId
    val isGroup: Boolean get() = !isPersonalSpace && !isPublicSharing
}

sealed interface GroupOrUserSpace {
    val id: String
    data class Group(val group: UserGroupWithMetadata): GroupOrUserSpace {
        override val id: String = group.id
    }
    data class PersonalSpace(val space: UserPersonalSpaceModel): GroupOrUserSpace {
        override val id: String = UserGroup.groupPersonalDevicesId
    }
}

object GroupOrSpaceIdHelper {
    fun wrap(id: String): GroupOrSpaceId {
        return GroupOrSpaceId(id = id)
    }

    fun unwrap(id: Any?): String? {
        return when (id) {
            is GroupOrSpaceId -> id.id
            else -> null
        }
    }
}

fun String.asGroupOrSpaceId() = GroupOrSpaceId(this)