package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.provider.IConfig
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString

class GroupValidityVerification(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val config: IConfig
) {

    private suspend fun verifyUser(
        groupId: String,
        memberUserId: String,
        memberVerification: String,
        groupKey: GroupMasterKey,
        groupOwnerPublicKey: GroupMemberMainPublicKey,
        memberPublicKey: GroupMemberMainPublicKey,
        memberGroupSettings: GroupSettingsRaw?,
    ): VerificationResult {
        try {
            if(groupOwnerPublicKey !is GroupMemberMainPublicKey.Valid)
                return VerificationResult.MISSING_GROUP_OWNER_PUBLIC_KEY

            if(memberPublicKey !is GroupMemberMainPublicKey.Valid)
                return VerificationResult.MISSING_PUBLIC_KEY

            if(groupKey !is GroupMasterKey.Valid)
                return VerificationResult.MISSING_GROUP_KEY

            val verification = try {
                cryptoCore.verifySignature(
                    data = memberVerification,
                    publicKey = groupOwnerPublicKey.key
                ) ?: return VerificationResult.WRONG_VERIFICATION_ENTRY_SIGNATURE
            } catch (e: Exception) {
                return VerificationResult.WRONG_VERIFICATION_ENTRY_FORM
            }

            if (memberUserId == config.userId) {
                val myKey = userKeyRepo.latestKeyPair()

                if (memberGroupSettings == null)
                    return VerificationResult.MISSING_MEMBER_SETTINGS

                if (memberGroupSettings.integrity.isBlank())
                    return VerificationResult.MISSING_MEMBER_INTEGRITY

                val hash = cryptoCore.hashProvider.computeHash(
                    data = myKey.publicKey,
                    type = HashType.SHA256
                )

                if(verification != hash)
                    return VerificationResult.WRONG_OWNER_VERIFICATION

                val userGroupKeys = cryptoKEKRepo.getMyPreKeyForGroup(groupId).key

                if (userGroupKeys == null)
                    return VerificationResult.MISSING_MEMBER_SECRET

                cryptoCore.verifySignature(
                    data = memberGroupSettings.integrity,
                    publicKey = myKey.publicKey
                )?.let {
                    try {
                        cryptoCore.decryptDataWithKey(
                            data = it,
                            key = userKeyRepo.personalKey,
                        ).let {
                            BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(
                                it
                            )
                        }
                    } catch (e: Exception) {
                        return VerificationResult.WRONG_MEMBER_INTEGRITY
                    }
                } ?: return VerificationResult.WRONG_MEMBER_INTEGRITY_SIGNATURE
            }

            return VerificationResult.VERIFIED
        } catch (e: Exception) {
            logg.tag("crypto_verification").e("Can not verify member", e)
            return VerificationResult.GENERIC_ERROR
        }
    }

    suspend fun verifyGroupMember(
        member: MemberDocumentRaw,
        groupKey: GroupMasterKey,
        groupOwnerId: String
    ): VerificationResult {
        val groupOwnerPublicKey = cryptoKEKRepo.getMemberPublicKey(
            groupId = member.groupId,
            memberUserId = groupOwnerId
        )
        val memberPublicKey = cryptoKEKRepo.getMemberPublicKey(
            groupId = member.groupId,
            memberUserId = member.ownerUserId
        )
        return verifyUser(
            groupId = member.groupId,
            memberUserId = member.ownerUserId,
            memberVerification = member.verification,
            groupKey = groupKey,
            groupOwnerPublicKey = groupOwnerPublicKey,
            memberPublicKey = memberPublicKey,
            memberGroupSettings = member.settingsRaw
        )
    }


    suspend fun checkIntegrityForGroupKey(group: UserGroupWithMembers): IntegrityResult {
        return checkIntegrity(
            groupId = group.group.id,
            myIntegrity = group.members.firstOrNull { it.member.userId == config.userId }?.member?.settings?.integrity,
            memberVerifications = group.members.map { it.member.verification },
            forGroup = true
        )
    }

    suspend fun checkIntegrityForMemberKey(group: UserGroupWithMembers): IntegrityResult {
        return checkIntegrity(
            groupId = group.group.id,
            myIntegrity = group.members.firstOrNull { it.member.userId == config.userId }?.member?.settings?.integrity,
            memberVerifications = group.members.map { it.member.verification },
            forGroup = false
        )
    }

    private suspend fun checkIntegrity(
        groupId: String,
        myIntegrity: String?,
        memberVerifications: List<String>,
        forGroup: Boolean,
    ): IntegrityResult {

        val tag = "integrity_check${if (forGroup) "_group" else "_member"}"
        if (myIntegrity.isNullOrBlank()) {
            logg.tag(tag).w("CORRUPTED for $groupId - Missing own integrity")
            return IntegrityResult.CORRUPTED
        }

        val remoteIntegrity: GroupIntegrity
        val realIntegritySignature = try {
            val myKey = userKeyRepo.latestKeyPair()
            remoteIntegrity = try {
                cryptoCore.decryptDataWithKeyPair(
                    data = myIntegrity,
                    publicKey = myKey.publicKey,
                    myPrivateKey = myKey.privateKey
                ).let { BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it) }
            } catch (e: Exception) {
                logg.tag(tag).w("CORRUPTED for $groupId - Error parsing: ${e.message}")
                return IntegrityResult.CORRUPTED
            }

            val groupKey = cryptoKEKRepo.getGroupKey(
                groupId = groupId,
                keyVersion = null
            ) as GroupMasterKey.Valid

            GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                memberVerifications = memberVerifications,
                groupKey = groupKey.key,
                encryptedMemberPublicKeys = cryptoKEKRepo.getAllEncryptedMemberPublicKeys(groupId)
            )
        } catch (e: Exception) {
            logg.tag(tag).w("CORRUPTED for $groupId - Generic error: ${e.message}")
            return IntegrityResult.CORRUPTED
        }

        val valid = if (forGroup) remoteIntegrity.isValidForGroup(realIntegritySignature)
        else remoteIntegrity.isValidForMember(realIntegritySignature)
        if (valid) {
            logg.tag(tag).v("VALID for $groupId")
            return IntegrityResult.VALID
        } else {
            logg.tag(tag).v("VALID_EXPIRED for $groupId")
            return IntegrityResult.VALID_EXPIRED
        }
    }
}

enum class IntegrityResult {
    VALID,
    VALID_EXPIRED,
    CORRUPTED
}

enum class VerificationResult {
    VERIFIED,
    GENERIC_ERROR,
    MISSING_GROUP_KEY,
    MISSING_PUBLIC_KEY,
    MISSING_GROUP_OWNER_PUBLIC_KEY,
    WRONG_VERIFICATION_ENTRY_SIGNATURE,
    WRONG_VERIFICATION_ENTRY_FORM,
    WRONG_OWNER_VERIFICATION,
    WRONG_MEMBER_VERIFICATION_SIGNATURE,
    WRONG_MEMBER_VERIFICATION_FORM,
    MISSING_MEMBER_SETTINGS,
    MISSING_MEMBER_INTEGRITY,
    WRONG_MEMBER_INTEGRITY,
    WRONG_MEMBER_INTEGRITY_SIGNATURE,
    MISSING_MEMBER_SECRET,
    WRONG_MEMBER_SECRET,
    INVALID_MEMBER_SECRET,
}