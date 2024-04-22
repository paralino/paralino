package app.paralino.core.domain.crypto

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.data.db.AppConfig
import app.paralino.core.logg
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.utils.BaseRemoteModule

class GroupValidityVerification(
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val config: AppConfig
) {

    private suspend fun verifyUser(
        memberUserId: String,
        memberVerification: String,
        groupKey: GroupMasterKey,
        groupOwnerPublicKey: GroupMemberMainPublicKey,
        memberPublicKey: GroupMemberMainPublicKey,
        memberGroupSettings: GroupSettingsRaw?,
    ): VerificationResult {
        val verificationErrors = mutableListOf<VerificationResultError>()
        try {
            when (groupOwnerPublicKey) {
                is GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair -> {
                    when (groupOwnerPublicKey.reason) {
                        UserGroupPersonalKeyPair.Error.Invalid -> VerificationResultError.GROUP_OWNER_INVALID_PERSONAL_KEY
                        UserGroupPersonalKeyPair.Error.Missing -> VerificationResultError.GROUP_OWNER_MISSING_PERSONAL_KEY
                    }
                }

                GroupMemberMainPublicKey.Error.InvalidPublicKey -> VerificationResultError.GROUP_OWNER_INVALID_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.Missing -> VerificationResultError.GROUP_OWNER_MISSING_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.UnverifiedPublicKey -> VerificationResultError.GROUP_OWNER_UNVERIFIED_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.WrongIdentity -> VerificationResultError.GROUP_OWNER_WRONG_IDENTITY
                GroupMemberMainPublicKey.Error.WrongPublicKey -> VerificationResultError.GROUP_OWNER_WRONG_PUBLIC_KEY
                is GroupMemberMainPublicKey.Valid -> null
            }?.also { verificationErrors.add(it) }

            when (memberPublicKey) {
                is GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair -> {
                    when (memberPublicKey.reason) {
                        UserGroupPersonalKeyPair.Error.Invalid -> VerificationResultError.MEMBER_INVALID_PERSONAL_KEY
                        UserGroupPersonalKeyPair.Error.Missing -> VerificationResultError.MEMBER_MISSING_PERSONAL_KEY
                    }
                }

                GroupMemberMainPublicKey.Error.InvalidPublicKey -> VerificationResultError.MEMBER_INVALID_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.Missing -> VerificationResultError.MEMBER_MISSING_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.UnverifiedPublicKey -> VerificationResultError.MEMBER_UNVERIFIED_PUBLIC_KEY
                GroupMemberMainPublicKey.Error.WrongIdentity -> VerificationResultError.MEMBER_WRONG_IDENTITY
                GroupMemberMainPublicKey.Error.WrongPublicKey -> VerificationResultError.MEMBER_WRONG_PUBLIC_KEY
                is GroupMemberMainPublicKey.Valid -> null
            }?.also { verificationErrors.add(it) }

            when (groupKey) {
                GroupMasterKey.Error.Invalid -> VerificationResultError.GROUP_KEY_INVALID
                is GroupMasterKey.Error.InvalidOwnerPublicKey -> {
                    when (groupKey.reason) {
                        is GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair -> {
                            when (groupKey.reason.reason) {
                                UserGroupPersonalKeyPair.Error.Invalid -> VerificationResultError.GROUP_KEY_INVALID_PERSONAL_KEY
                                UserGroupPersonalKeyPair.Error.Missing -> VerificationResultError.GROUP_KEY_MISSING_PERSONAL_KEY
                            }
                        }

                        GroupMemberMainPublicKey.Error.InvalidPublicKey -> VerificationResultError.GROUP_KEY_INVALID_PUBLIC_KEY
                        GroupMemberMainPublicKey.Error.Missing -> VerificationResultError.GROUP_KEY_MISSING_PUBLIC_KEY
                        GroupMemberMainPublicKey.Error.UnverifiedPublicKey -> VerificationResultError.GROUP_KEY_UNVERIFIED_PUBLIC_KEY
                        GroupMemberMainPublicKey.Error.WrongIdentity -> VerificationResultError.GROUP_KEY_WRONG_IDENTITY
                        GroupMemberMainPublicKey.Error.WrongPublicKey -> VerificationResultError.GROUP_KEY_WRONG_PUBLIC_KEY
                    }
                }

                is GroupMasterKey.Error.InvalidPersonalKeyPair -> {
                    when (groupKey.reason) {
                        UserGroupPersonalKeyPair.Error.Invalid -> VerificationResultError.GROUP_KEY_INVALID_PERSONAL_KEY
                        UserGroupPersonalKeyPair.Error.Missing -> VerificationResultError.GROUP_KEY_MISSING_PERSONAL_KEY
                    }
                }

                GroupMasterKey.Error.Missing -> VerificationResultError.GROUP_KEY_MISSING
                is GroupMasterKey.Valid -> null
            }?.also { verificationErrors.add(it) }


            val verification = try {
                if (memberVerification.isBlank()) {
                    verificationErrors.add(VerificationResultError.MEMBER_VERIFICATION_MISSING)
                } else if (groupOwnerPublicKey is GroupMemberMainPublicKey.Valid) {
                    cryptoCore.verifySignature(
                        data = memberVerification,
                        publicKey = groupOwnerPublicKey.identityKey.key
                    ) ?: let {
                        verificationErrors.add(VerificationResultError.WRONG_MEMBER_VERIFICATION_SIGNATURE)
                        null
                    }
                } else {
                    verificationErrors.add(VerificationResultError.GROUP_OWNER_MISSING_PUBLIC_KEY)
                    null
                }
            } catch (e: Exception) {
                verificationErrors.add(VerificationResultError.WRONG_MEMBER_VERIFICATION_FORM)
                null
            }

            if (memberPublicKey is GroupMemberMainPublicKey.Valid) {
                val hash = cryptoCore.hashProvider.computeHash(
                    data = memberPublicKey.identityKey.key,
                    type = HashType.SHA256
                )

                if (verification != hash) {
                    verificationErrors.add(VerificationResultError.WRONG_OWNER_VERIFICATION)
                }
            } else {
                verificationErrors.add(VerificationResultError.MEMBER_MISSING_PUBLIC_KEY)
            }

            if (memberUserId == config.userId) {
                if (memberGroupSettings == null) {
                    verificationErrors.add(VerificationResultError.MISSING_MEMBER_SETTINGS)
                } else {
                    if (memberGroupSettings.integrity.isBlank()) {
                        verificationErrors.add(VerificationResultError.MISSING_MEMBER_INTEGRITY)
                    } else {
                        try {
                            cryptoCore.decryptDataWithKey(
                                data = memberGroupSettings.integrity,
                                key = userKeyRepo.personalKey(),
                            ).let {
                                try {
                                    BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it)
                                } catch (e: Exception) {
                                    verificationErrors.add(VerificationResultError.INVALID_MEMBER_INTEGRITY_FORMAT)
                                }
                            }
                        } catch (e: Exception) {
                            verificationErrors.add(VerificationResultError.WRONG_MEMBER_INTEGRITY)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logg.tag("crypto_verification").e("Can not verify member", e)
            verificationErrors.add(VerificationResultError.GENERIC_ERROR)
        }

        if (verificationErrors.isEmpty()) {
            return VerificationResult.Verified
        } else {
            return VerificationResult.Unverified(errors = verificationErrors)
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
            memberUserId = member.ownerUserId,
            memberVerification = member.verification,
            groupKey = groupKey,
            groupOwnerPublicKey = groupOwnerPublicKey,
            memberPublicKey = memberPublicKey,
            memberGroupSettings = member.settingsRaw
        )
    }

    suspend fun checkIntegrityForGroupKey(group: UserGroupWithMembers): IntegrityResult {
        val integrity = getIntegrity(
            groupId = group.group.id,
            serverIntegrity = group.group.serverIntegrity,
            myIntegrity = group.members.firstOrNull { it.member.userId == config.userId }?.member?.settings?.integrity,
            memberVerifications = group.members.map { it.member.verification },
            forGroup = true
        )
            ?: return IntegrityResult.CORRUPTED

        if (integrity.first.isValidForGroup(integrity.second)) {
            logg.tag("integrity_check_member").v("VALID for ${group.id}")
            return IntegrityResult.VALID
        } else if (integrity.first.signature != integrity.second) {
            logg.tag("integrity_check_member").v("VALID_CHANGED for ${group.id}")
            return IntegrityResult.VALID_CHANGED
        } else {
            logg.tag("integrity_check_member").v("VALID_EXPIRED for ${group.id}")
            return IntegrityResult.VALID_EXPIRED
        }
    }

    suspend fun checkIntegrityForMemberKey(group: UserGroupWithMembers): IntegrityResult {
        val integrity = getIntegrity(
            groupId = group.group.id,
            serverIntegrity = group.group.serverIntegrity,
            myIntegrity = group.members.firstOrNull { it.member.userId == config.userId }?.member?.settings?.integrity,
            memberVerifications = group.members.map { it.member.verification },
            forGroup = false
        )
            ?: return IntegrityResult.CORRUPTED

        if (integrity.first.isValidForMember(integrity.second)) {
            logg.tag("integrity_check_group").v("VALID for ${group.id}")
            return IntegrityResult.VALID
        } else if (integrity.first.signature != integrity.second) {
            logg.tag("integrity_check_group").v("VALID_CHANGED for ${group.id}")
            return IntegrityResult.VALID_CHANGED
        } else {
            logg.tag("integrity_check_group").v("VALID_EXPIRED for ${group.id}")
            return IntegrityResult.VALID_EXPIRED
        }
    }

    suspend fun checkIntegrityForDeviceKey(group: UserGroupWithMembers): IntegrityResult {
        val keyIntegrity = cryptoKEKRepo.getDeviceIntegrityForGroup(groupId = group.id)
        if (keyIntegrity.isNullOrBlank()) {
            logg.tag("integrity_check_device").v("VALID_EXPIRED for ${group.id} (missing)")
            return IntegrityResult.VALID_EXPIRED
        }

        val integrity = getIntegrity(
            groupId = group.group.id,
            serverIntegrity = group.group.serverIntegrity,
            myIntegrity = keyIntegrity,
            memberVerifications = group.members.map { it.member.verification },
            forGroup = false
        )
            ?: return IntegrityResult.CORRUPTED

        if (integrity.first.isValidForDevice(integrity.second)) {
            logg.tag("integrity_check_device").v("VALID for ${group.id}")
            return IntegrityResult.VALID
        } else if (integrity.first.signature != integrity.second) {
            logg.tag("integrity_check_device").v("VALID_CHANGED for ${group.id}")
            return IntegrityResult.VALID_CHANGED
        } else {
            logg.tag("integrity_check_device").v("VALID_EXPIRED for ${group.id}")
            return IntegrityResult.VALID_EXPIRED
        }
    }

    private suspend fun getIntegrity(
        groupId: String,
        serverIntegrity: String,
        myIntegrity: String?,
        memberVerifications: List<String>,
        forGroup: Boolean,
    ): Pair<GroupIntegrity, String>? {
        val tag = "integrity_check${if (forGroup) "_group" else "_member"}"
        if (myIntegrity.isNullOrBlank()) {
            logg.tag(tag).w("CORRUPTED for $groupId - Missing own integrity")
            return null
        }

        val remoteIntegrity = try {
            cryptoCore.decryptDataWithKey(
                data = myIntegrity,
                key = userKeyRepo.personalKey()
            ).let { BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it) }
        } catch (e: Exception) {
            logg.tag(tag).w("CORRUPTED for $groupId - Error parsing: ${e.message}")
            return null
        }

        val realIntegritySignature = try {
            GroupIntegrity.createSignature(
                hashProvider = cryptoCore.hashProvider,
                serverIntegrity = serverIntegrity,
                memberVerifications = memberVerifications,
                encryptedMemberPublicKeys = cryptoKEKRepo.getAllEncryptedMembershipPublicKeys(
                    groupId = groupId
                )
            )
        } catch (e: Exception) {
            logg.tag(tag).w("CORRUPTED for $groupId - Generic error: ${e.message}")
            return null
        }

        return remoteIntegrity to realIntegritySignature
    }
}

enum class IntegrityResult {
    VALID,
    VALID_EXPIRED,
    VALID_CHANGED,
    CORRUPTED;

    val shouldChange: Boolean get() = this == VALID_EXPIRED || this == VALID_CHANGED
}

sealed interface VerificationResult {
    data object Verified : VerificationResult
    data class Unverified(val errors: List<VerificationResultError>) : VerificationResult {
        override fun toString(): String {
            return "Unverified(${errors.joinToString(separator = ", ")})"
        }
    }
}

enum class VerificationResultError {
    MEMBER_INVALID_PUBLIC_KEY,
    MEMBER_MISSING_PUBLIC_KEY,
    MEMBER_WRONG_PUBLIC_KEY,
    MEMBER_UNVERIFIED_PUBLIC_KEY,
    MEMBER_WRONG_IDENTITY,
    MEMBER_MISSING_PERSONAL_KEY,
    MEMBER_INVALID_PERSONAL_KEY,
    MEMBER_VERIFICATION_MISSING,

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