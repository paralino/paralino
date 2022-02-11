package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.core.models.mappers.IDataMapperLocal
import com.zbapp.locationtrack.data.models.invite.InviteCodeEntity

class InviteCodeMapper(
    private val cryptoCore: CryptoCore
) : IDataMapperLocal<InviteCodeEntity, PersonalCodeData> {

    override suspend fun local2Remote(model: InviteCodeEntity): PersonalCodeData {
        return cryptoCore.decryptModel(model.data, cryptoCore.loadUserPersonalKey().content)
    }

    override suspend fun remote2Local(model: PersonalCodeData): InviteCodeEntity {
        return InviteCodeEntity(
            id = model.id,
            ownerId = model.ownerId,
            data = cryptoCore.encryptModel(model, cryptoCore.loadUserPersonalKey().content)
        )
    }

}