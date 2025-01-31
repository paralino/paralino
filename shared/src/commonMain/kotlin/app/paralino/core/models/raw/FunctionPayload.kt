package app.paralino.core.models.raw

import kotlinx.serialization.Serializable


@Serializable
data class CreateGroupPayload constructor(
    val encryptedGroupKey: String,
    val signedPublicKey: String,
    val ownerIdentityKey: String,
    val groupMemberIntegrity: String,
    val userKeyVersion: Long,
    val serverIntegrity: String,
    val encryptedUserKeys: String,
    val keyId: String,
    val memberVerification: String,
    val groupData: String,
    val memberData: String,
    val paralinoId: String?
)

@Serializable
data class UpdateGroupPayload constructor(
    val groupId: String,
    val groupData: String
)

@Serializable
data class CreateGroupMemberPayload constructor(
    val groupId: String,
    val codeKeyId: String,
    val keyId: String,
    val groupOwnerIntegrity: String,
    val userKeyVersion: Long,
    val serverIntegrity: String,
    val memberVerification: String,
    val userToAddId: String,
    val groupKeys: List<GroupMemberGroupKeysData>,
    val memberSignedPublicKey: String,
    val memberIdentityKey: String,
    val memberData: String,
    val paralinoId: String?
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
    val groupData: String,
    val userKeyVersion: Long
)

@Serializable
data class BlockGroupMemberFunctionData(
    val groupId: String,
    val userId: String,
    val userData: String,
    val userKeyVersion: Long
)

@Serializable
data class UpdateGroupKeysPayload constructor(
    val groupId: String,
    val integrity: String,
    val userKeyVersion: Long,
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
    val encryptedGroupKey: String,
    val memberKeyVersion: Long
)

@Serializable
data class GroupMemberPublicKeyPayload(
    val userId: String,
    val signedPublicKey: String,
    val ownerIdentityKey: String,
)

@Serializable
data class UpdateGroupMemberSenderKeysPayload(
    val groupId: String,
    val locationData: MemberLocationDataPayload,
)

@Serializable
data class UpdateOwnMemberPublicKeyPayload(
    val groupId: String,
    val integrity: String,
    val userKeyVersion: Long,
    val memberData: String,
    val paralinoId: String?,
    val signedPublicKey: String,
    val encryptedMemberPersonalKeys: String,
    val groupKeyVersion: Long,
    val memberKeyVersionsToKeep: List<Long>
)

@Serializable
data class MemberLocationDataPayload constructor(
    val locationId: String,
    val deviceId: String,
    val groupKeyVersion: Long,
    val deviceData: String,
    val locationData: String,
    val status: String,
    val encryptedSenderKeys: List<GroupSenderKeyPayload>,
)

@Serializable
data class GroupSenderKeyPayload constructor(
    val userId: String,
    val memberKeyVersion: Long,
    val encryptedSenderKey: String,
)

@Serializable
data class UpdateMemberFunctionPayload constructor(
    val data: List<UpdateMemberData>
)

@Serializable
data class UpdateMemberData constructor(
    val memberId: String,
    val groupKeyVersion: Long,
    val memberData: String,
)

@Serializable
data class CreatePlacePayload(
    val groupId: String,
    val keyVersion: Long,
    val private: Boolean,
    val data: String,
    val eventKeys: List<String>
)

@Serializable
data class UpdatePlacePayload constructor(
    val placeId: String,
    val placeData: String,
    val groupKeyVersion: Long,
    val eventKeys: List<String>?
)

@Serializable
data class PlaceEventPayload constructor(
    val placeId: String,
    val locationId: String?,
    val enter: Boolean?,
    val event: String?,
    val timeDifferenceMillis: Long
)

@Serializable
data class UpdateUserKeysFunctionData constructor(
    val encryptedPublicKey: String,
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val publicKey: String,
    val oldMasterKeys: String?,
    val loginSalt: String,
    val loginAlg: String,
    val dataAlg: String,
    val matchingKeyVersion: Long,
    val removeOldKeys: Boolean,
)

@Serializable
data class AccountResetCryptoKeyPayload constructor(
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val encryptedPublicKey: String,
    val publicKey: String,
    val loginSalt: String,
    val loginAlg: String,
    val dataAlg: String
)

@Serializable
data class LocationCreatePayload constructor(
    val groupId: String,
    val deviceId: String,
    val deviceData: String?,
    val locationData: String?,
    val status: String,
    val groupKeyVersion: Long,
    val notify: Boolean,
    val keys: List<GroupSenderKeyPayload>
)

@Serializable
data class LocationDeletePayload constructor(
    val memberId: String,
    val locationId: String,
)

@Serializable
data class UpdateUserDeviceFunctionData constructor(
    val sessionId: String,
    val deviceId: String,
    val pushToken: String,
    val deviceType: String,
    val locationPushToken: String?
)

@Serializable
data class PersonalCodeUpdateData(
    val keyId: String,
    val encryptedCodeUserData: String,
    val keys: List<PersonalCodePreKeyData>
)

@Serializable
data class PersonalCodeCreateData(
    val keyId: String,
    val encryptedCode: String,
    val encryptedCodeUserData: String,
    val userKeyVersion: Long,
    val keys: List<PersonalCodePreKeyData>
)

@Serializable
data class PersonalCodePreKeyData(
    val encryptedCodeKeyData: String?,
    val encryptedUserKeys: String,
    val integrity: String,
    val userKeyVersion: Long,
    val keyId: String
)

@Serializable
data class PersonalPreKeyUpdateData(
    val personalCodeKeyId: String,
    val keys: List<PersonalCodePreKeyData>
)

@Serializable
data class PersonalCodeResponse constructor(
    val userId: String,
    val encryptedCodeUserData: String,
    val encryptedCodeKeyData: String
)

@Serializable
data class LocationRequestPayload constructor(
    val groupId: String,
    val userId: String,
    val locationId: String,
)

@Serializable
data class LocationFocusPayload constructor(
    val groupId: String,
    val userId: String?,
    val locationId: String?
)

@Serializable
data class PromoCodeRedeemPayload(
    val code: String
)

@Serializable
data class BatteryNotificationsUpdatePayload(
    val groupId: String,
    val memberId: String,
    val alertData: String
)

@Serializable
data class PlaceNotificationsUpdatePayload(
    val groupId: String,
    val placeId: String,
    val placeSettingsId: String?,
    val alertData: String
)

@Serializable
data class BatteryAlertPayload constructor(
    val groupId: String,
    val memberId: String,
    val locationId: String
)

@Serializable
data class PersonalCodeReadPayload(
    val keyId: String
)