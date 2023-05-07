package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.UserGroupPersonalKeyPair
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

            when(groupOwnerPublicKey) {
                is GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair -> {
                    return when(groupOwnerPublicKey.reason) {
                        UserGroupPersonalKeyPair.Error.Invalid -> VerificationResult.GROUP_OWNER_INVALID_PERSONAL_KEY
                        UserGroupPersonalKeyPair.Error.Missing -> VerificationResult.GROUP_OWNER_MISSING_PERSONAL_KEY
                    }
                }
                GroupMemberMainPublicKey.Error.InvalidPublicKey -> return VerificationResult.GROUP_OWNER_INVALID_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.Missing -> return VerificationResult.GROUP_OWNER_MISSING_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.UnverifiedPublicKey -> return VerificationResult.GROUP_OWNER_UNVERIFIED_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.WrongIdentity -> return VerificationResult.GROUP_OWNER_WRONG_IDENTITY
                GroupMemberMainPublicKey.Error.WrongPublicKey -> return VerificationResult.GROUP_OWNER_WRONG_PUBLIC_KEY
                is GroupMemberMainPublicKey.Valid -> {}
            }

            when(memberPublicKey) {
                is GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair -> {
                    return when(memberPublicKey.reason) {
                        UserGroupPersonalKeyPair.Error.Invalid -> VerificationResult.MEMBER_INVALID_PERSONAL_KEY
                        UserGroupPersonalKeyPair.Error.Missing -> VerificationResult.MEMBER_MISSING_PERSONAL_KEY
                    }
                }
                GroupMemberMainPublicKey.Error.InvalidPublicKey -> return VerificationResult.MEMBER_INVALID_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.Missing -> return VerificationResult.MEMBER_MISSING_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.UnverifiedPublicKey -> return VerificationResult.MEMBER_UNVERIFIED_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.WrongIdentity -> return VerificationResult.MEMBER_WRONG_IDENTITY
                GroupMemberMainPublicKey.Error.WrongPublicKey -> return VerificationResult.MEMBER_WRONG_PUBLIC_KEY
                is GroupMemberMainPublicKey.Valid -> {}
            }

            when(groupKey) {
                GroupMasterKey.Error.Invalid -> return VerificationResult.GROUP_KEY_INVALID
                is GroupMasterKey.Error.InvalidOwnerPublicKey -> {
                    return when(groupKey.reason) {
                        is GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair -> {
                            when(groupKey.reason.reason) {
                                UserGroupPersonalKeyPair.Error.Invalid -> VerificationResult.GROUP_KEY_INVALID_PERSONAL_KEY
                                UserGroupPersonalKeyPair.Error.Missing -> VerificationResult.GROUP_KEY_MISSING_PERSONAL_KEY
                            }
                        }
                        GroupMemberMainPublicKey.Error.InvalidPublicKey -> VerificationResult.GROUP_KEY_INVALID_PUBLIC_KEY
                        GroupMemberMainPublicKey.Error.Missing -> VerificationResult.GROUP_KEY_MISSING_PUBLIC_KEY
                        GroupMemberMainPublicKey.Error.UnverifiedPublicKey -> VerificationResult.GROUP_KEY_UNVERIFIED_PUBLIC_KEY
                        GroupMemberMainPublicKey.Error.WrongIdentity -> VerificationResult.GROUP_KEY_WRONG_IDENTITY
                        GroupMemberMainPublicKey.Error.WrongPublicKey -> VerificationResult.GROUP_KEY_WRONG_PUBLIC_KEY
                    }
                }
                is GroupMasterKey.Error.InvalidPersonalKeyPair -> {
                    return when(groupKey.reason) {
                        UserGroupPersonalKeyPair.Error.Invalid -> VerificationResult.GROUP_KEY_INVALID_PERSONAL_KEY
                        UserGroupPersonalKeyPair.Error.Missing -> VerificationResult.GROUP_KEY_MISSING_PERSONAL_KEY
                    }
                }
                GroupMasterKey.Error.Missing -> return VerificationResult.GROUP_KEY_MISSING
                is GroupMasterKey.Valid -> {}
            }

            val verification = try {
                cryptoCore.verifySignature(
                    data = memberVerification,
                    publicKey = groupOwnerPublicKey.identityKey.key
                ) ?: return VerificationResult.WRONG_MEMBER_VERIFICATION_SIGNATURE
            } catch (e: Exception) {
                return VerificationResult.WRONG_MEMBER_VERIFICATION_FORM
            }

            val hash = cryptoCore.hashProvider.computeHash(
                data = memberPublicKey.identityKey.key,
                type = HashType.SHA256
            )

            if(verification != hash)
                return VerificationResult.WRONG_OWNER_VERIFICATION

            if (memberUserId == config.userId) {

                if (memberGroupSettings == null)
                    return VerificationResult.MISSING_MEMBER_SETTINGS

                if (memberGroupSettings.integrity.isBlank())
                    return VerificationResult.MISSING_MEMBER_INTEGRITY

                try {
                    cryptoCore.decryptDataWithKey(
                        data = memberGroupSettings.integrity,
                        key = userKeyRepo.personalKey,
                    ).let {
                        try {
                            BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it)
                        } catch (e: Exception) {
                            return VerificationResult.INVALID_MEMBER_INTEGRITY_FORMAT
                        }
                    }
                } catch (e: Exception) {
                    return VerificationResult.WRONG_MEMBER_INTEGRITY
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
            remoteIntegrity = try {
                cryptoCore.decryptDataWithKey(
                    data = myIntegrity,
                    key = userKeyRepo.personalKey
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

    MEMBER_INVALID_PUBLIC_KEY,
    MEMBER_MISSING_PUBLIC_KEY,
    MEMBER_WRONG_PUBLIC_KEY,
    MEMBER_UNVERIFIED_PUBLIC_KEY,
    MEMBER_WRONG_IDENTITY,
    MEMBER_MISSING_PERSONAL_KEY,
    MEMBER_INVALID_PERSONAL_KEY,

    GROUP_OWNER_INVALID_PUBLIC_KEY,
    GROUP_OWNER_MISSING_PUBLIC_KEY,
    GROUP_OWNER_WRONG_PUBLIC_KEY,
    GROUP_OWNER_UNVERIFIED_PUBLIC_KEY,
    GROUP_OWNER_WRONG_IDENTITY,
    GROUP_OWNER_MISSING_PERSONAL_KEY,
    GROUP_OWNER_INVALID_PERSONAL_KEY,

    GROUP_KEY_INVALID_PUBLIC_KEY,
    GROUP_KEY_MISSING_PUBLIC_KEY,
    GROUP_KEY_WRONG_PUBLIC_KEY,
    GROUP_KEY_UNVERIFIED_PUBLIC_KEY,
    GROUP_KEY_WRONG_IDENTITY,
    GROUP_KEY_MISSING_PERSONAL_KEY,
    GROUP_KEY_INVALID_PERSONAL_KEY,
    GROUP_KEY_INVALID,
    GROUP_KEY_MISSING,

    WRONG_OWNER_VERIFICATION,
    WRONG_MEMBER_VERIFICATION_SIGNATURE,
    WRONG_MEMBER_VERIFICATION_FORM,
    MISSING_MEMBER_SETTINGS,
    MISSING_MEMBER_INTEGRITY,
    WRONG_MEMBER_INTEGRITY,
    INVALID_MEMBER_INTEGRITY_FORMAT,
    GENERIC_ERROR
}