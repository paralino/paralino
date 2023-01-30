package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.logg
import app.paralino.core.models.location.MemberVerification
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.IConfig
import kotlinx.serialization.decodeFromString

class GroupValidityVerification(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val config: IConfig
) {

    suspend fun verifyUser(
        groupId: String,
        memberUserId: String,
        groupKey: Data?,
        memberVerification: String,
        groupOwnerPublicKey: String?,
    ): VerificationResult {
        try {
            groupOwnerPublicKey?.takeIf { it.isNotBlank() } ?: return VerificationResult.MISSING_PUBLIC_KEY
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
                    data = verification.groupOwnerSecret,
                    publicKey = groupOwnerPublicKey
                ) ?: return VerificationResult.WRONG_OWNER_VERIFICATION
            } catch (e: Exception) {
                return VerificationResult.WRONG_OWNER_VERIFICATION
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

            if(memberUserId == config.userId) {
                // TODO maybe check for personal code content
            }

            return VerificationResult.VERIFIED
        } catch (e: Exception) {
            logg.tag("crypto_verification").e("Can not verify member", e)
            return VerificationResult.GENERIC_ERROR
        }
    }

    suspend fun verifyUser(
        groupId: String,
        memberUserId: String,
        groupKeyVersion: Long,
        memberVerification: String,
        groupOwnerPublicKey: String?,
    ): VerificationResult {
        val groupKey = try {
            cryptoKEKRepo.getGroupKey(
                groupId = groupId,
                keyVersion = groupKeyVersion
            )?.data
        } catch (e: Exception) {
            null
        }
        return verifyUser(
            groupId = groupId,
            memberUserId = memberUserId,
            groupKey = groupKey,
            memberVerification = memberVerification,
            groupOwnerPublicKey = groupOwnerPublicKey
        )
    }

    suspend fun verifyGroupMember(
        member: MemberDocumentRaw,
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
            groupKeyVersion = member.groupKeyVersion,
            groupOwnerPublicKey = groupOwnerPublicKey
        )
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
            groupKey = groupKey
        )
    }
}
enum class VerificationResult {
    VERIFIED,
    GENERIC_ERROR,
    MISSING_GROUP_KEY,
    MISSING_PUBLIC_KEY,
    WRONG_VERIFICATION_ENTRY_SIGNATURE,
    WRONG_VERIFICATION_ENTRY_FORM,
    WRONG_OWNER_VERIFICATION,
    WRONG_MEMBER_VERIFICATION
}