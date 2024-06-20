package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.PersonalCodeKeyData
import app.paralino.core.models.user.PersonalCodeUserData
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
            key = userKeyRepo.personalKey()
        )

        val derivedCode = cryptoCore.derivePassword(code)

        val userData = if (!model.encryptedCodeUserData.isNullOrBlank()) {
            try {
                CryptoResource.Ready(
                    cryptoCore.decryptDataWithSimplePassword(
                        data = model.encryptedCodeUserData,
                        password = derivedCode.backupPassword,
                        userId = model.ownerUserId
                    ).let {
                        BaseRemoteModule.formatter.decodeFromString<PersonalCodeUserData>(it)
                    }.takeIf {
                        it.ownerIdentityKey == userKeyRepo.latestKeyPair().publicKey
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