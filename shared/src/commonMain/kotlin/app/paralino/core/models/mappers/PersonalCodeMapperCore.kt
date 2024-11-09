package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.PersonalCodeKeyData
import app.paralino.core.models.user.PersonalCodeUserData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException

class PersonalCodeMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
) {

    suspend fun remote2CoreRead(
        code: String,
        encryptedUserData: String,
        encryptedKeyData: String,
        userId: String
    ): UserPersonalCodeData {

        val derivedCode = cryptoCore.deriveParalinoIdMasterKeys(
            code = code,
            userId = userId
        )

        val userData = cryptoCore.decryptDataWithKey(
            data = encryptedUserData,
            key = derivedCode.backupPassword
        ).let {
            BaseRemoteModule.formatter.decodeFromString<PersonalCodeUserData>(it)
        }

        val keyData = cryptoCore.decryptDataWithKey(
            data = encryptedKeyData,
            key = derivedCode.backupPassword
        ).let {
            BaseRemoteModule.formatter.decodeFromString<PersonalCodeKeyData>(it)
        }

        if (userId != userData.ownerUserId) throw DecryptionException("User personal code can not be verified")
        if (code != keyData.code) throw DecryptionException("User personal code can not be verified")

        cryptoCore.verifySignature(
            data = keyData.signedOwnerKxPublicKey,
            publicKey = userData.ownerIdentityKey
        ) ?: throw DecryptionException("User personal code can not be verified")

        return UserPersonalCodeData(
            codeKeyId = derivedCode.loginPassword,
            userData = userData,
            keyData = keyData
        )
    }

    suspend fun mapMyCode(model: PersonalCodeRaw): UserPersonalCode {

        val code = cryptoCore.decryptDataWithKey(
            data = model.encryptedCode,
            key = userKeyRepo.personalKey(version = model.userKeyVersion).key
        )

        val derivedCode = cryptoCore.deriveParalinoIdMasterKeys(
            code = code,
            userId = model.ownerUserId
        )

        val userData = if (!model.encryptedCodeUserData.isNullOrBlank()) {
            try {
                CryptoResource.Ready(
                    cryptoCore.decryptDataWithKey(
                        data = model.encryptedCodeUserData,
                        key = derivedCode.backupPassword
                    ).let {
                        BaseRemoteModule.formatter.decodeFromString<PersonalCodeUserData>(it)
                    }.takeIf {
                        it.ownerIdentityKey == userKeyRepo.signatureKeyPair().publicKey
                    }!!
                )
            } catch (e: Exception) {
                logg.w("personal_code", "Error verifying personal code", e)
                CryptoResource.WrongKeyOrData
            }
        } else {
            CryptoResource.NotInitialized
        }

        return UserPersonalCode(
            id = model.id,
            code = code,
            codeData = userData,
        )
    }
}