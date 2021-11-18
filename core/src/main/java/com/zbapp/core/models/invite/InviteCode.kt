package com.zbapp.core.models.invite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InviteCode private constructor(

    @SerialName("\$id")
    val id: String,

    /**
     * since api function currently can't return value, we have to poll backend for data
     * so we will poll for this value
     */
    val tempId: String,

    val ownerId: String,
    val ownerName: String,
    val ownerImageBase64: String,

    val groupId: String?,
    val groupName: String?,
    val groupEmoji: String?,
    val groupMembersCount: Int?,

    val inviteCode: String,
    val inviteUrl: String,

    val validity: Long,
    val userLimit: Int
) {

    fun matchesInviteCode(code: String): Boolean {
        return inviteCode
            .replace("-", "")
            .replace(" ", "")
            .equals(
                code.replace("-", "")
                    .replace(" ", ""),
                ignoreCase = true
            )
    }
    val isGroupInvite get() = !groupId.isNullOrBlank()

    companion object {
        fun appInvite(
            ownerId: String,
            ownerName: String,
            ownerImageBase64: String,
            userLimit: Int = 0
        ): InviteCode {
            return InviteCode(
                id = "",
                tempId = "",
                ownerId = ownerId,
                ownerName = ownerName,
                ownerImageBase64 = ownerImageBase64,
                groupId = null,
                groupName = null,
                groupEmoji = null,
                groupMembersCount = null,
                inviteCode = "",
                inviteUrl = "",
                validity = 0,
                userLimit = userLimit
            )
        }

        fun groupInvite(
            ownerId: String,
            ownerName: String,
            ownerImageBase64: String,
            groupId: String?,
            groupName: String?,
            groupEmoji: String?,
            groupMembersCount: Int?,
            userLimit: Int = 0
        ): InviteCode {
            return InviteCode(
                id = "",
                tempId = "",
                ownerId = ownerId,
                ownerName = ownerName,
                ownerImageBase64 = ownerImageBase64,
                groupId = groupId,
                groupName = groupName,
                groupEmoji = groupEmoji,
                groupMembersCount = groupMembersCount,
                inviteCode = "",
                inviteUrl = "",
                validity = 0,
                userLimit = userLimit
            )
        }
    }
}
