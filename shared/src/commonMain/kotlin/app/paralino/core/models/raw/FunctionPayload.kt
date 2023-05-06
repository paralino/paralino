package app.paralino.core.models.raw

import kotlinx.serialization.Serializable


@Serializable
data class GenericFunctionPayload<T>(
    val route: String,
    val payload: T
)

@Serializable
data class CreateGroupPayload constructor(
    val encryptedGroupKey: String,
    val encryptedSenderKey: String,
    val encryptedPublicKey: String,
    val ownerIdentityKey: String,
    val groupMemberIntegrity: String,
    val encryptedUserKeys: String,
    val memberVerification: String,
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
    val keyId: String,
    val groupOwnerIntegrity: String,
    val memberVerification: String,
    val userToAddId: String,
    val encryptedSenderKey: String,
    val groupKeys: List<GroupMemberGroupKeysData>,
    val keysForNewUser: List<GroupMemberPublicKeyPayload>,
    val userKeyForOtherUsers: List<GroupMemberPublicKeyPayload>,
    val memberData: String
)

@Serializable
data class GroupMemberGroupKeysData constructor(
    val version: Long,
    val encryptedGroupKey: String,
)

@Serializable
data class DeleteGroupMemberPayload constructor(
    val groupId: String,
    val userId: String
)

@Serializable
data class BlockGroupFunctionData(
    val groupId: String,
    val groupData: String
)

@Serializable
data class BlockGroupMemberFunctionData(
    val groupId: String,
    val userId: String,
    val userData: String
)

@Serializable
data class UpdateGroupKeysPayload constructor(
    val groupId: String,
    val integrity: String,
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
data class GroupMemberPublicKeyPayload(
    val userId: String,
    val encryptedPublicKey: String,
    val ownerIdentityKey: String,
)


@Serializable
data class UpdateGroupMemberKeysPayload(
    val groupId: String,
    val deleteOldKeys: Boolean,
    val encryptedSenderKeys: List<GroupMemberKeyPayload>,
    val memberData: GroupMemberDataPayload?,
)

@Serializable
data class GroupMemberKeyPayload constructor(
    val userId: String,
    val encryptedPublicKey: String?,
    val encryptedSenderKey: String,
    val keyVersion: Long? = null
)

@Serializable
data class GroupMemberDataPayload constructor(
    val memberData: String,
    val integrity: String,
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
data class UpdateMemberFunctionPayload constructor(
    val data: List<UpdateMemberData>
)

@Serializable
data class UpdateMemberData constructor(
    val memberId: String,
    val locationId: String,
    val groupKeyVersion: Long,
    val memberData: String,
    val deviceData: String
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
    val locationId: String?,
    val enter: Boolean
)

@Serializable
data class UpdateUserKeysFunctionData constructor(
    val encryptedPublicKey: String,
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val keyVersion: Long,
    val removeOldKeys: Boolean,
)

@Serializable
data class AccountResetCryptoKeyPayload constructor(
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val encryptedPublicKey: String,
)

@Serializable
data class LocationCreatePayload constructor(
    val groupId: String,
    val deviceId: String,
    val deviceData: String?,
)

@Serializable
data class LocationDeletePayload constructor(
    val memberId: String,
    val locationId: String,
)

@Serializable
data class UpdateUserDeviceFunctionData constructor(
    val sessionId: String,
    val pushToken: String,
    val deviceType: String
)

@Serializable
data class PersonalCodeUpdateData(
    val keyId: String,
    val encryptedCodeUserData: String?,
    val keys: List<PersonalCodePreKeyData>
)

@Serializable
data class PersonalCodeCreateData(
    val keyId: String,
    val encryptedCode: String,
    val encryptedCodeUserData: String,
    val keys: List<PersonalCodePreKeyData>
)

@Serializable
data class PersonalCodePreKeyData(
    val encryptedCodeKeyData: String?,
    val encryptedUserKeys: String,
    val integrity: String
)

@Serializable
data class PersonalCodeResponse constructor(
    val userId: String,
    val encryptedCodeUserData: String,
    val encryptedCodeKeyData: String
)