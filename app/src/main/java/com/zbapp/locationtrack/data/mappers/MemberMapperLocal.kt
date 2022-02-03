package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.mappers.MemberMapperCore
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.locationtrack.data.models.location.MemberEntity

class MemberMapperLocal(
    private val coreMapper: MemberMapperCore
) : IDataMapperLocal<MemberEntity, MemberDocumentRaw> {

    suspend fun local2Core(model: MemberEntity, ownerPublicKey: String): MemberModel {
        return coreMapper.remote2Core(local2Remote(model), ownerPublicKey)
    }

    override suspend fun local2Remote(model: MemberEntity): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            ownerPublicKey = model.ownerPublicKey,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            data = model.data,
            mutableData = model.mutableData
        )
    }

    override suspend fun remote2Local(model: MemberDocumentRaw): MemberEntity {
        return MemberEntity(
            id = model.id,
            userId = model.ownerUserId,
            groupId = model.groupId,
            ownerPublicKey = model.ownerPublicKey,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            data = model.data,
            mutableData = model.mutableData
        )
    }

}
