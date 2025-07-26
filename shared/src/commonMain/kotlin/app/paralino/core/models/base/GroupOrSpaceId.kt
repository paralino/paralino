package app.paralino.core.models.base

import app.paralino.core.models.group.UserGroup
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

fun String.asGroupOrSpaceId() = GroupOrSpaceId(this)