package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.mappers.IMemberMapperCore
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.base.IGroupCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.locationtrack.data.models.location.MemberEntity

class MemberMapper(
    private val crypto: IGroupCrypto
) : IMemberMapperCore, IDataMapperLocal<MemberEntity, MemberDocumentRaw> {

    override suspend fun core2Remote(model: MemberModel): MemberDocumentRaw {
        val locationData = crypto.encryptModel(groupId = model.id, data = model.data)
        return MemberDocumentRaw(
            id = model.id,
            ownerId = model.userId,
            groupId = model.groupId,
            data = crypto.encryptModel(model.groupId, model.copy(data = null)) ?: "",
            mutableData = locationData
        )
    }

    override suspend fun local2Remote(model: MemberEntity): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerId = model.userId,
            groupId = model.groupId,
            data = model.data,
            mutableData = model.mutableData
        )
    }

    override suspend fun remote2Core(model: MemberDocumentRaw): MemberModel {
        val locationData = crypto.decryptModel<LocationDataModel>(
            groupId = model.groupId ?: model.id,
            data = model.mutableData,
            senderId = model.ownerId
        )

        return crypto.decryptModel<MemberModel>(
            groupId = model.groupId ?: model.id,
            data = model.data,
            senderId = model.ownerId
        )!!.copy(id = model.id, data = locationData)
    }

    override suspend fun remote2Local(model: MemberDocumentRaw): MemberEntity {
        return MemberEntity(
            id = model.id,
            userId = model.ownerId,
            groupId = model.groupId,
            data = model.data,
            mutableData = model.mutableData
        )
    }

}
