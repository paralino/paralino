package app.paralino.android.data.mappers

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.android.data.models.group.UserGroupEntity
import app.paralino.core.domain.crypto.VerificationResult

class GroupsMapperLocal(
    private val coreMapper: GroupMapperCore
) : IDataMapperLocal<UserGroupEntity, GroupDocumentRaw> {

    suspend fun local2Core(model: UserGroupEntity, memberCount: Int = 0, verification: VerificationResult): UserGroup {
        return coreMapper.remote2Core(local2Remote(model), memberCount, verification)
    }

    override suspend fun local2Remote(model: UserGroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            integrity = model.integrity,
            data = model.data
        )
    }

    override suspend fun remote2Local(model: GroupDocumentRaw): UserGroupEntity {
        return UserGroupEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            keyVersion = model.keyVersion,
            shouldRotateKeys = model.shouldRotateKeys,
            integrity = model.integrity,
            data = model.data
        )
    }
}