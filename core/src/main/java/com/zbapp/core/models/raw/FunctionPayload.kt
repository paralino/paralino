package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable


@Serializable
data class CreateGroupPostData constructor(
    val userPublicKey: String,
    val encryptedGroupKey: String,
    val encryptedMemberKey: String,
    val groupData: String,
    val memberData: String
)

@Serializable
data class CreateGroupMemberFunctionData constructor(
    val groupId: String,
    val userId: String,
    val userPublicKey: String,
    val encryptedGroupKey: String,
    val encryptedOwnerKey: String,
    val oldKeys: List<GroupMemberOldKeysData>,
    val memberData: String
)

@Serializable
data class GroupMemberOldKeysData constructor(
    val version: Long,
    val encryptedGroupKey: String,
)

@Serializable
data class DeleteGroupMemberFunctionData constructor(
    val groupId: String,
    val userId: String
)

@Serializable
data class UpdateGroupKeysFunctionData(
    val groupId: String,
    val userPublicKey: String,
    val encryptedGroupKeys: List<GroupKeysPayload>,
    val groupData: String
)

@Serializable
data class GroupKeysPayload(
    val userId: String,
    val encryptedGroupKey: String
)


@Serializable
data class UpdateGroupMemberKeysFunctionData(
    val groupId: String,
    val userPublicKey: String,
    val encryptedSenderKeys: List<GroupMemberKeyPayload>,
    val memberData: List<GroupMemberDataPayload>
)

@Serializable
data class GroupMemberKeyPayload(
    val userId: String,
    val encryptedSenderKey: String
)

@Serializable
data class GroupMemberDataPayload(
    val memberId: String,
    val groupKeyVersion: Long,
    val memberData: String,
    val mutableMemberData: String
)