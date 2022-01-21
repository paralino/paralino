package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
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
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            membersAccessKeys = null,
            ownerPublicKey = "",
            data = crypto.encryptModel(model.groupId, model.data) ?: "",
            mutableData = crypto.encryptModel(groupId = model.id, data = model.data)
        )
    }

    override suspend fun local2Remote(model: MemberEntity): MemberDocumentRaw {
        return MemberDocumentRaw(
            id = model.id,
            ownerUserId = model.userId,
            groupId = model.groupId,
            membersAccessKeys = null,
            ownerPublicKey = "",
            data = model.data,
            mutableData = model.mutableData
        )
    }

    override suspend fun remote2Core(model: MemberDocumentRaw): MemberModel {
        val locationData = crypto.decryptModel<LocationDataModel>(
            groupId = model.groupId ?: model.id,
            data = model.mutableData,
            senderId = model.ownerUserId
        )

        val memberData = crypto.decryptModel<MemberModelData>(
            groupId = model.groupId ?: model.id,
            data = model.data,
            senderId = model.ownerUserId
        )!!

        return MemberModel(
            id = model.id,
            groupId = model.groupId ?: model.id,
            userId = model.ownerUserId,
            data = memberData,
            mutableData = locationData,
            publicKey = model.ownerPublicKey
        )
    }

    override suspend fun remote2Local(model: MemberDocumentRaw): MemberEntity {
        return MemberEntity(
            id = model.id,
            userId = model.ownerUserId,
            groupId = model.groupId,
            data = model.data,
            mutableData = model.mutableData
        )
    }

}
