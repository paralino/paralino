package app.paralino.core.models.raw

import kotlinx.serialization.Serializable


@Serializable
data class CreateGroupPayload constructor(
    val userPublicKey: String,
    val encryptedGroupKey: String,
    val encryptedMemberKey: String,
    val groupData: String,
    val memberData: String
)

@Serializable
data class UpdateGroupPayload constructor(
    val groupId: String,
    val groupData: String
)

@Serializable
data class CreateGroupMemberPayload constructor(
    val groupId: String,
    val userId: String,
    val userPublicKey: String,
    val encryptedGroupKey: String,
    val encryptedOwnerKey: String,
    val oldKeys: List<GroupMemberOldKeyPayload>,
    val memberData: String
)

@Serializable
data class GroupMemberOldKeyPayload constructor(
    val version: Long,
    val encryptedGroupKey: String,
)

@Serializable
data class DeleteGroupMemberPayload constructor(
    val groupId: String,
    val userId: String
)

@Serializable
data class UpdateGroupKeysPayload constructor(
    val groupId: String,
    val userPublicKey: String,
    val encryptedGroupKeys: List<GroupKeysPayload>,
    val groupData: String,
    val places: List<GroupPlaceDataUpdatePayload>
)

@Serializable
data class GroupPlaceDataUpdatePayload(
    val placeId: String,
    val placeData: String
)

@Serializable
data class GroupKeysPayload(
    val userId: String,
    val encryptedGroupKey: String
)


@Serializable
data class UpdateGroupMemberKeysPayload(
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

@Serializable
data class UpdateLocationFunctionData(
    val data: List<LocationNodePayload>
)

@Serializable
data class LocationNodePayload(
    val memberId: String,
    val groupId: String,
    val mutableData: String?,
    val memberKeyVersion: Long
)

@Serializable
data class CreatePlacePayload(
    val groupId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val private: Boolean,
    val data: String
)

@Serializable
data class PlaceEventPayload(
    val placeId: String,
    val enter: Boolean
)