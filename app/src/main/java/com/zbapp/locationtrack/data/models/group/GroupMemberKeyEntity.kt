package com.zbapp.locationtrack.data.models.group

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_member_keys")
data class GroupMemberKeyEntity constructor(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val memberUserId: String,
    val ownerUserId: String,
    val time: Long,
    // owner sender key K encrypted with member public key, signed with owner private key
    val encryptedSenderKey: String
)
