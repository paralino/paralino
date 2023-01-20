package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
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

        return codeData
    }

    suspend fun remote2Core(model: PersonalCodeRaw): UserPersonalCode {

        val code = cryptoCore.decryptDataWithKey(
            data = model.encryptedCode,
            key = userKeyRepo.personalKey.content
        )

        val codeData = remote2CoreRead(code, model.encryptedCodeData)

        val userOwnSecret = cryptoCore.decryptDataWithKey(
            data = model.verification,
            key = userKeyRepo.personalKey.content
        )

        val verifiedKey = cryptoCore.verifySignature(
            data = userOwnSecret,
            publicKey = userKeyRepo.latestKeyPair().publicKey
        )

        if(userOwnSecret != codeData.secretKey || verifiedKey == null)
            throw DecryptionException("User personal code can not be verified")

        return UserPersonalCode(
            id = model.id,
            validityUTC = model.validityUTC,
            verification = model.verification,
            data = codeData
        )
    }

}