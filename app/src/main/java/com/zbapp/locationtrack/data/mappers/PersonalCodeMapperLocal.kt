package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.user.UserPersonalCode
import com.zbapp.core.models.mappers.IDataMapperLocal
import com.zbapp.core.models.mappers.PersonalCodeMapperCore
import com.zbapp.core.models.raw.PersonalCodeRaw
import com.zbapp.locationtrack.data.models.user.PersonalCodeEntity

class PersonalCodeMapperLocal(
    private val coreMapper: PersonalCodeMapperCore,
) : IDataMapperLocal<PersonalCodeEntity, PersonalCodeRaw> {

    suspend fun local2Core(model: PersonalCodeEntity): UserPersonalCode {
        return coreMapper.remote2Core(local2Remote(model))
    }

    override suspend fun local2Remote(model: PersonalCodeEntity): PersonalCodeRaw {
        return PersonalCodeRaw(
            id = model.id,
            keyId = model.keyId,
            ownerId = model.ownerId,
            encryptedCode = model.encryptedCode,
            encryptedCodeData = model.encryptedCodeData,
            validityUnix = model.validityUnix
        )
    }

    override suspend fun remote2Local(model: PersonalCodeRaw): PersonalCodeEntity {
        return PersonalCodeEntity(
            id = model.id,
            keyId = model.keyId,
            ownerId = model.ownerId,
            encryptedCode = model.encryptedCode,
            encryptedCodeData = model.encryptedCodeData,
            validityUnix = model.validityUnix
        )
    }

}