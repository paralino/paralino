package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.GroupIntegrity
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.PersonalCodeKeyData
import app.paralino.core.models.user.PersonalCodeUserData
import app.paralino.core.models.user.SimpleKeyPairData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException
import kotlinx.serialization.decodeFromString

class PersonalCodeMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
) {

    suspend fun remote2CoreRead(
        code: String,
        encryptedUserData: String,
        encryptedKeyData: String,
        userId: String
    ): UserPersonalCodeData {

        val derivedCode = cryptoCore.derivePassword(code)

        val userData = cryptoCore.decryptDataWithSimplePassword(
            data = encryptedUserData,
            password = derivedCode.backupPassword,
            userId = userId
        ).let {
            BaseRemoteModule.formatter.decodeFromString<PersonalCodeUserData>(it)
        }

        val keyData = cryptoCore.decryptDataWithSimplePassword(
            data = encryptedKeyData,
            password = derivedCode.backupPassword,
            userId = userId
        ).let {
            BaseRemoteModule.formatter.decodeFromString<PersonalCodeKeyData>(it)
        }

        if(userId != userData.ownerUserId) throw DecryptionException("User personal code can not be verified")
        if(code != keyData.code) throw DecryptionException("User personal code can not be verified")

        cryptoCore.verifySignature(
            data = keyData.memberVerificationSecret,
            publicKey = userData.ownerIdentityKey
        ) ?: throw DecryptionException("User personal code can not be verified")

        cryptoCore.verifySignature(
            data = keyData.signedOwnerKxPublicKey,
            publicKey = userData.ownerIdentityKey
        ) ?: throw DecryptionException("User personal code can not be verified")

        return UserPersonalCodeData(
            userData = userData,
            keyData = keyData
        )
    }

    suspend fun mapMyCode(model: PersonalCodeRaw): UserPersonalCode {

        val code = cryptoCore.decryptDataWithKey(
            data = model.encryptedCode,
            key = userKeyRepo.personalKey
        )

        var codeData = try {
            if (!model.isStillValid()) {
                CryptoResource.NotInitialized
            } else {
                CryptoResource.Ready(
                    remote2CoreRead(
                        code = code,
                        encryptedUserData = model.encryptedCodeUserData!!,
                        encryptedKeyData = model.encryptedCodeKeyData!!,
                        userId = model.ownerUserId
                    ).takeIf {
                        it.userData.ownerIdentityKey == userKeyRepo.latestKeyPair().publicKey
                    }!!
                )
            }
        } catch (e: Exception) {
            logg.w("personal_code", "Error verifying personal code", e)
            CryptoResource.WrongKeyOrData
        }

        codeData = when (codeData) {
            is CryptoResource.Ready -> {

                if (model.encryptedUserKeys.isNullOrBlank() || model.integrity.isNullOrBlank()) {
                    // this will never execute since code validity is checked above in isStillValid function
                    CryptoResource.WrongKeyOrData
                } else {
                    try {

                        val userKeys = cryptoCore.decryptDataWithKey(
                            data = model.encryptedUserKeys,
                            key = userKeyRepo.personalKey
                        ).let { BaseRemoteModule.formatter.decodeFromString<SimpleKeyPairData>(it) }

                        val hash = cryptoCore.hashProvider.computeHash(
                            data = userKeys.publicKey,
                            type = HashType.SHA256
                        )

                        val hashFromCodeData = cryptoCore.verifySignature(
                            data = codeData.data.keyData.memberVerificationSecret,
                            publicKey = userKeyRepo.latestKeyPair().publicKey
                        )

                        val integrityCheck = cryptoCore.verifySignature(
                            data = model.integrity,
                            publicKey = userKeyRepo.latestKeyPair().publicKey
                        )?.let {
                            try {
                                cryptoCore.decryptDataWithKeyPair(
                                    data = it,
                                    myPrivateKey = userKeys.privateKey,
                                    publicKey = userKeys.publicKey
                                ).let {
                                    BaseRemoteModule.formatter.decodeFromString<GroupIntegrity>(it)
                                }
                            } catch (e: Exception) {
                                logg.w("personal_code", "Personal code integrity is invalid")
                                null
                            }
                        }

                        if (hash != hashFromCodeData || integrityCheck == null)
                            CryptoResource.WrongKeyOrData
                        else
                            codeData
                    } catch (e: Exception) {
                        logg.w("personal_code", "Personal code verification failed")
                        CryptoResource.WrongKeyOrData
                    }
                }
            }
            else -> codeData
        }

        return UserPersonalCode(
            id = model.id,
            code = code,
            encryptedUserKeys = model.encryptedUserKeys,
            integrity = model.integrity,
            codeData = codeData,
        )
    }
}