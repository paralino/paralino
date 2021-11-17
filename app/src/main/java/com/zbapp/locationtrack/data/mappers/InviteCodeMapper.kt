package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.group.IUserCrypto
import com.zbapp.core.repo.group.decryptModel
import com.zbapp.core.repo.group.encryptModel
import com.zbapp.locationtrack.data.models.invite.InviteCodeEntity

class InviteCodeMapper(
    private val crypto: IUserCrypto
) : IDataMapperLocal<InviteCodeEntity, InviteCode> {

    override suspend fun local2Remote(model: InviteCodeEntity): InviteCode {
        return crypto.decryptModel<InviteCode>(model.data)!!
    }

    override suspend fun remote2Local(model: InviteCode): InviteCodeEntity {
        return InviteCodeEntity(
            id = model.id,
            ownerId = model.ownerId,
            data = crypto.encryptModel(model) ?: ""
        )
    }

}