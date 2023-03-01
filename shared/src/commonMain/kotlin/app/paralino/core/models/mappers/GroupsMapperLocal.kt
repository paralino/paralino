package app.paralino.core.models.mappers

import app.paralino.core.data.db.GroupEntity
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.GroupDocumentRaw

class GroupsMapperLocal(
    private val coreMapper: GroupMapperCore
) {

    suspend fun local2Core(
        model: GroupEntity,
        memberCount: Int = 0,
        verification: VerificationResult
    ): UserGroup {
        return coreMapper.remote2Core(local2Remote(model), memberCount, verification)
    }

    fun local2Remote(model: GroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            data = model.data_,
            planType = model.planType
        )
    }

    fun remote2Local(model: GroupDocumentRaw): GroupEntity {
        return GroupEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            data_ = model.data,
            planType = model.planType
        )
    }
}