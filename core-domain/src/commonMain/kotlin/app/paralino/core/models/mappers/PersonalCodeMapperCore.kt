package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.AppHost
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.PersonalCodeKeyData
import app.paralino.core.models.user.PersonalCodeUserData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException

class PersonalCodeMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val appConfig: AppConfigRepository
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

        if (userId != userData.ownerUserId) throw DecryptionException("personal_code", "User personal code can not be verified")
        if (code != keyData.code) throw DecryptionException("personal_code", "User personal code can not be verified")

        cryptoCore.verifySignature(
            data = keyData.signedOwnerKxPublicKey,
            publicKey = userData.ownerIdentityKey
        ) ?: throw DecryptionException("personal_code", "User personal code can not be verified")

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

        // TODO this triggers Argon2 key derivation on every change and every app start
        // and it can make app very slow
        // skip or encrypted with user master key which doesn't need constant key derivation
        val derivedCode = if (appConfig.appHost == AppHost.MAIN_APP) {
            cryptoCore.deriveParalinoIdMasterKeys(
                code = code,
                userId = model.ownerUserId
            )
        } else {
            null
        }

        val userData = if (!model.encryptedCodeUserData.isNullOrBlank() && derivedCode?.backupPassword?.isNotBlank() == true) {
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