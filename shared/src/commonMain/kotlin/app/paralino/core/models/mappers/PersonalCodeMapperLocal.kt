package app.paralino.core.models.mappers

import app.paralino.core.db.main.user.PersonalCodeEntity
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.raw.PersonalCodeRaw

class PersonalCodeMapperLocal(
    private val coreMapper: PersonalCodeMapperCore,
) {

    suspend fun local2Core(model: PersonalCodeEntity): UserPersonalCode {
        return coreMapper.mapMyCode(local2Remote(model))
    }

    fun local2Remote(model: PersonalCodeEntity): PersonalCodeRaw {
        return PersonalCodeRaw(
            id = model.id,
            keyId = model.keyId,
            ownerUserId = model.ownerUserId,
            encryptedCode = model.encryptedCode,
            encryptedCodeUserData = model.encryptedCodeUserData,
            userKeyVersion = model.userKeyVersion
        )
    }

    fun remote2Local(model: PersonalCodeRaw): PersonalCodeEntity {
        return PersonalCodeEntity(
            id = model.id,
            keyId = model.keyId,
            ownerUserId = model.ownerUserId,
            encryptedCode = model.encryptedCode,
            encryptedCodeUserData = model.encryptedCodeUserData,
            userKeyVersion = model.userKeyVersion
        )
    }
}