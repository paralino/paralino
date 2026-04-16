package app.paralino.core.models.base

import kotlin.jvm.JvmInline

@JvmInline
value class GroupOrSpaceId(val id: String) {
    val isPersonalSpace: Boolean get() = id == groupPersonalDevicesId
    val isPublicSharing: Boolean get() = id == groupPublicSharingId
    val isGroup: Boolean get() = !isPersonalSpace && !isPublicSharing

    companion object {
        const val groupPublicSharingId = "public_sharing"
        const val groupPersonalDevicesId = "personal_devices"
        const val groupPrivateGroupId = "privateGroup"
    }
}

object GroupOrSpaceIdHelper {
    fun wrap(id: String): GroupOrSpaceId {
        return GroupOrSpaceId(id = id)
    }

    fun unwrap(id: Any?): String? {
        return when (id) {
            is GroupOrSpaceId -> id.id
            is String -> id
            is CharSequence -> id.toString()
            else -> null
        }
    }
}

fun String.asGroupOrSpaceId() = GroupOrSpaceId(this)