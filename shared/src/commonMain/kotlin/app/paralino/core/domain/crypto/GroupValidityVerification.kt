package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.base64ToData
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberVerification
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.provider.IConfig
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
        groupKey: Data?,
        memberVerification: String,
        groupOwnerPublicKey: String?,
        memberGroupSettings: GroupSettingsRaw?,
    ): VerificationResult {
        try {
            groupOwnerPublicKey?.takeIf { it.isNotBlank() }
                ?: return VerificationResult.MISSING_GROUP_OWNER_PUBLIC_KEY
            groupKey ?: return VerificationResult.MISSING_GROUP_KEY

            val verification = try {
                cryptoCore.verifySignature(
                    data = memberVerification,
                    publicKey = groupOwnerPublicKey
                )?.let {
                    BaseRemoteModule.formatter.decodeFromString<MemberVerification>(it)
                } ?: return VerificationResult.WRONG_VERIFICATION_ENTRY_SIGNATURE
            } catch (e: Exception) {
                return VerificationResult.WRONG_VERIFICATION_ENTRY_FORM
            }

            val secretKey = try {
                cryptoCore.verifySignature(
                    data = verification.memberSecret,
                    publicKey = cryptoKEKRepo.getMemberPublicKey(
                        groupId = groupId,
                        memberUserId = memberUserId
                    ) ?: return VerificationResult.MISSING_PUBLIC_KEY
                ) ?: return VerificationResult.WRONG_MEMBER_VERIFICATION_SIGNATURE
            } catch (e: Exception) {
                return VerificationResult.WRONG_MEMBER_VERIFICATION_FORM
            }

            if (memberUserId == config.userId) {
                if (memberGroupSettings == null)
                    return VerificationResult.MISSING_MEMBER_SETTINGS

                if (memberGroupSettings.integrity.isBlank()) {
                    return VerificationResult.MISSING_MEMBER_INTEGRITY
                } else {
                    val myKey = userKeyRepo.latestKeyPair()
                    try {
                        cryptoCore.decryptDataWithKeyPair(
                            data = memberGroupSettings.integrity,
                            publicKey = myKey.publicKey,
                            myPrivateKey = myKey.privateKey
                        ).let {
                            BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it)
                        }
                    } catch (e: Exception) {
                        return VerificationResult.WRONG_MEMBER_INTEGRITY
                    }
                }

                if (memberGroupSettings.secretKey.isBlank()) {
                    return VerificationResult.MISSING_MEMBER_SECRET
                } else {
                    try {
                        val userOwnSecret = cryptoCore.decryptDataWithKey(
                            data = memberGroupSettings.secretKey,
                            key = userKeyRepo.personalKey.content
                        )

                        val hash = cryptoCore.hashProvider.computeHash(
                            data = userOwnSecret.base64ToData(cryptoCore.hashProvider).content,
                            type = HashType.SHA256
                        ).asBase64String(cryptoCore.hashProvider)

                        if (hash != secretKey)
                            return VerificationResult.WRONG_MEMBER_SECRET
                    } catch (e: Exception) {
                        return VerificationResult.INVALID_MEMBER_SECRET
                    }
                }
            }

            return VerificationResult.VERIFIED
        } catch (e: Exception) {
            logg.tag("crypto_verification").e("Can not verify member", e)
            return VerificationResult.GENERIC_ERROR
        }
    }

    suspend fun verifyGroupMember(
        member: MemberDocumentRaw,
        groupKey: Data?,
        groupOwnerId: String
    ): VerificationResult {
        val groupOwnerPublicKey = cryptoKEKRepo.getMemberPublicKey(
            groupId = member.groupId,
            memberUserId = groupOwnerId
        )
        return verifyUser(
            groupId = member.groupId,
            memberUserId = member.ownerUserId,
            memberVerification = member.verification,
            groupOwnerPublicKey = groupOwnerPublicKey,
            groupKey = groupKey,
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

        val tag = "integrity_check${if(forGroup) "_group" else "_member"}"
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
            )!!.data

            GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                memberVerifications = memberVerifications,
                groupKey = groupKey.asBase64String(cryptoCore.hashProvider),
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
    MISSING_MEMBER_SECRET,
    WRONG_MEMBER_SECRET,
    INVALID_MEMBER_SECRET,
}