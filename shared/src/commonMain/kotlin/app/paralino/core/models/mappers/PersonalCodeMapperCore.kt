package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.base64ToData
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException
import kotlinx.serialization.decodeFromString

class PersonalCodeMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
) {

    suspend fun remote2CoreRead(code: String, encryptedCodeData: String): UserPersonalCodeData {

        val derivedCode = cryptoCore.derivePassword(code)

        val codeData = cryptoCore.decryptDataWithSimplePassword(
            data = encryptedCodeData,
            password = derivedCode.backupPassword
        ).let {
            BaseRemoteModule.formatter.decodeFromString<UserPersonalCodeData>(it)
        }

        cryptoCore.verifySignature(
            data = codeData.secretKey,
            publicKey = codeData.ownerPublicKey
        ) ?: throw DecryptionException("User personal code can not be verified")

        return codeData
    }

    suspend fun mapMyCode(model: PersonalCodeRaw): UserPersonalCode {

        val code = cryptoCore.decryptDataWithKey(
            data = model.encryptedCode,
            key = userKeyRepo.personalKey.content
        )

        val codeData = remote2CoreRead(code, model.encryptedCodeData)

        val userOwnSecret = cryptoCore.decryptDataWithKey(
            data = model.verification,
            key = userKeyRepo.personalKey.content
        )

        val hash = cryptoCore.hashProvider.computeHash(
            data = userOwnSecret.base64ToData(cryptoCore.hashProvider).content,
            type = HashType.SHA256
        ).asBase64String(cryptoCore.hashProvider)

        val hashFromCodeData = cryptoCore.verifySignature(
            data = codeData.secretKey,
            publicKey = userKeyRepo.latestKeyPair().publicKey
        )

        if(hash != hashFromCodeData)
            throw DecryptionException("User personal code can not be verified")

        return UserPersonalCode(
            id = model.id,
            validityUTC = model.validityUTC,
            verification = model.verification,
            data = codeData
        )
    }

}