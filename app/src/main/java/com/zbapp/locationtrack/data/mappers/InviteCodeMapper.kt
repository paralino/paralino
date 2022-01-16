package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.core.repo.base.IUserCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.locationtrack.data.models.invite.InviteCodeEntity

class InviteCodeMapper(
    private val crypto: IUserCrypto
) : IDataMapperLocal<InviteCodeEntity, PersonalCodeData> {

    override suspend fun local2Remote(model: InviteCodeEntity): PersonalCodeData {
        return crypto.decryptModel<PersonalCodeData>(model.data)!!
    }

    override suspend fun remote2Local(model: PersonalCodeData): InviteCodeEntity {
        return InviteCodeEntity(
            id = model.id,
            ownerId = model.ownerId,
            data = crypto.encryptModel(model) ?: ""
        )
    }

}