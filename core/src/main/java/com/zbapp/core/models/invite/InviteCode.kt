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
    val ownerKey: String,

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

}

data class PersonalCodeData constructor(
    val ownerId: String,
    val ownerName: String,
    val ownerImageBase64: String,
    val ownerKey: String
) {

}