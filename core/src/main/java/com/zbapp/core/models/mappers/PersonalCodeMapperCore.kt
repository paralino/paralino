package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.DerivedPasswords
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.models.user.UserPersonalCode
import com.zbapp.core.models.user.UserPersonalCodeData
import com.zbapp.core.models.raw.PersonalCodeRaw
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class PersonalCodeMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
) {

    fun deriveCodeToKeys(code: String): DerivedPasswords {
        return cryptoCore.derivePassword(code)
    }

    suspend fun remote2CoreRead(code: String, encryptedCodeData: String): UserPersonalCodeData {

        val derivedCode = deriveCodeToKeys(code)

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

        return UserPersonalCode(
            id = model.id,
            validityUnix = model.validityUnix,
            data = codeData
        )
    }

    suspend fun core2Remote(model: UserPersonalCode): PersonalCodeRaw {

        val code = model.data.code
        val derivedCode = deriveCodeToKeys(code)

        return PersonalCodeRaw(
            id = model.id,
            keyId = derivedCode.loginPassword,
            encryptedCodeData = cryptoCore.encryptDataWithSimplePassword(
                data = BaseRemoteModule.formatter.encodeToString(model.data),
                password = derivedCode.backupPassword
            ),
            encryptedCode = cryptoCore.encryptDataWithKey(
                data = code,
                key = userKeyRepo.personalKey.content
            ),
            validityUnix = model.validityUnix
        )
    }

}