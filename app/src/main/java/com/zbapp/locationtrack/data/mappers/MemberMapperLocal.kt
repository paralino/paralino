package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.locationtrack.data.models.location.MemberEntity

object MemberMapperLocal : IDataMapperLocal<MemberEntity, MemberDocumentRaw> {

    override suspend fun local2Remote(model: MemberEntity): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            ownerPublicKey = model.ownerPublicKey,
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
            data = model.data,
            mutableData = model.mutableData
        )
    }

}
