package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.MemberVerification
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.IConfig
import kotlinx.serialization.decodeFromString

class GroupValidityVerification(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val cryptoUserKeyRepo: CryptoUserKeyRepository,
    private val config: IConfig
) {

    private suspend fun verifyUser(
        groupId: String,
        memberUserId: String,
        groupKey: Data?,
        memberVerification: String,
        groupOwnerPublicKey: String?,
        memberGroupIntegrity: String?
    ): VerificationResult {
        try {
            groupOwnerPublicKey?.takeIf { it.isNotBlank() }
                ?: return VerificationResult.MISSING_PUBLIC_KEY
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

            try {
                cryptoCore.verifySignature(
                    data = verification.memberSecret,
                    publicKey = cryptoKEKRepo.getMemberPublicKey(
                        groupId = groupId,
                        memberUserId = memberUserId
                    ) ?: return VerificationResult.MISSING_PUBLIC_KEY
                ) ?: return VerificationResult.WRONG_MEMBER_VERIFICATION
            } catch (e: Exception) {
                return VerificationResult.WRONG_MEMBER_VERIFICATION
            }

            if (memberUserId == config.userId) {
                if (memberGroupIntegrity.isNullOrBlank()) {
                    return VerificationResult.WRONG_MEMBER_INTEGRITY
                } else {
                    val myKey = cryptoUserKeyRepo.latestKeyPair()
                    try {
                        cryptoCore.decryptDataWithKeyPair(
                            data = memberGroupIntegrity,
                            publicKey = myKey.publicKey,
                            myPrivateKey = myKey.privateKey
                        ).let {
                            BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it)
                        }
                    } catch (e: Exception) {
                        return VerificationResult.WRONG_MEMBER_INTEGRITY
                    }
                }

                // TODO maybe check for personal code content
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
            memberGroupIntegrity = member.integrity
        )
    }


    suspend fun checkIntegrityForGroup(group: UserGroupWithMembers): IntegrityResult {
        return checkIntegrity(
            groupId = group.group.id,
            myIntegrity = group.members.firstOrNull() { it.member.userId == config.userId }?.member?.integrity,
            memberVerifications = group.members.map { it.member.verification },
            forGroup = true
        )
    }

    suspend fun checkIntegrityForMember(group: UserGroupWithMembers): IntegrityResult {
        return checkIntegrity(
            groupId = group.group.id,
            myIntegrity = group.members.firstOrNull() { it.member.userId == config.userId }?.member?.integrity,
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

        if (myIntegrity.isNullOrBlank())
            return IntegrityResult.CORRUPTED

        val remoteIntegrity: GroupIntegrity
        val realIntegritySignature = try {
            val myKey = cryptoUserKeyRepo.latestKeyPair()
            remoteIntegrity = try {
                cryptoCore.decryptDataWithKeyPair(
                    data = myIntegrity,
                    publicKey = myKey.publicKey,
                    myPrivateKey = myKey.privateKey
                ).let { BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it) }
            } catch (e: Exception) {
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
            return IntegrityResult.CORRUPTED
        }

        val valid = if(forGroup) remoteIntegrity.isValidForGroup(realIntegritySignature)
            else remoteIntegrity.isValidForMember(realIntegritySignature)
        if (valid) {
            return IntegrityResult.VALID
        } else {
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
    WRONG_VERIFICATION_ENTRY_SIGNATURE,
    WRONG_VERIFICATION_ENTRY_FORM,
    WRONG_OWNER_VERIFICATION,
    WRONG_MEMBER_VERIFICATION,
    WRONG_MEMBER_INTEGRITY,
}