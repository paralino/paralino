package app.paralino.android.data.mappers

import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.mappers.PersonalCodeMapperCore
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.android.data.models.user.PersonalCodeEntity

class PersonalCodeMapperLocal(
    private val coreMapper: PersonalCodeMapperCore,
) : IDataMapperLocal<PersonalCodeEntity, PersonalCodeRaw> {

    suspend fun local2Core(model: PersonalCodeEntity): UserPersonalCode {
        return coreMapper.mapMyCode(local2Remote(model))
    }

    override suspend fun local2Remote(model: PersonalCodeEntity): PersonalCodeRaw {
        return PersonalCodeRaw(
            id = model.id,
            keyId = model.keyId,
            ownerUserId = model.ownerUserId,
            encryptedCode = model.encryptedCode,
            encryptedCodeData = model.encryptedCodeData,
            validityUTC = model.validityUTC,
            verification = model.verification
        )
    }

    override suspend fun remote2Local(model: PersonalCodeRaw): PersonalCodeEntity {
        return PersonalCodeEntity(
            id = model.id,
            keyId = model.keyId,
            ownerUserId = model.ownerUserId,
            encryptedCode = model.encryptedCode,
            encryptedCodeData = model.encryptedCodeData,
            validityUTC = model.validityUTC,
            verification = model.verification
        )
    }

}