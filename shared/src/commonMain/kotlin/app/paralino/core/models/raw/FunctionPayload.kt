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
    val keyVersionsToKeep: List<Long>,
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
    val deleteOldKeys: Boolean,
    val encryptedSenderKeys: List<GroupMemberKeyPayload>,
    val memberData: GroupMemberDataPayload?,
)

@Serializable
data class GroupMemberKeyPayload constructor(
    val userId: String,
    val encryptedSenderKey: String,
    val keyVersion: Long? = null
)

@Serializable
data class GroupMemberDataPayload constructor(
    val memberData: String,
    val locations: List<MemberLocationDataPayload>,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long? = null,
)

@Serializable
data class MemberLocationDataPayload constructor(
    val locationId: String,
    val deviceData: String,
    val locationData: String,
)

@Serializable
data class UpdateLocationFunctionData(
    val data: List<LocationNodePayload>
)

@Serializable
data class LocationNodePayload(
    val groupId: String,
    val memberData: String? = null,
    val memberKeyVersion: Long,
    val groupKeyVersion: Long,
    val locations: List<LocationsPayload>,
)

@Serializable
data class LocationsPayload(
    val locationId: String?,
    val deviceData: String?,
    val locationData: String?
)

@Serializable
data class CreatePlacePayload(
    val groupId: String,
    val keyVersion: Long,
    val private: Boolean,
    val data: String
)

@Serializable
data class UpdatePlacePayload constructor(
    val placeId: String,
    val placeData: String
)

@Serializable
data class PlaceEventPayload constructor(
    val placeId: String,
    val memberId: String,
    val enter: Boolean
)

@Serializable
data class UpdateUserKeysFunctionData constructor(
    val userPublicKey: String,
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val keyVersion: Long,
    val removeOldKeys: Boolean,
)

@Serializable
data class AccountResetCryptoKeyPayload constructor(
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val publicKey: String,
)