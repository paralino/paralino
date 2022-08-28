package app.paralino.android.data.mappers

import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.mappers.MemberMapperCore
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.android.data.models.location.MemberEntity

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
            shouldRotateKeys = model.shouldRotateKeys,
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
            shouldRotateKeys = model.shouldRotateKeys,
            data = model.data,
            mutableData = model.mutableData
        )
    }

}
