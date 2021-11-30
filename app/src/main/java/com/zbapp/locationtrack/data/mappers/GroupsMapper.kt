package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.group.IGroupCrypto
import com.zbapp.core.repo.group.decryptModel
import com.zbapp.core.repo.group.encryptModel
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

class GroupsMapper(
    private val crypto: IGroupCrypto
) : IGroupDataMapperCore, IDataMapperLocal<UserGroupEntity, GroupDocumentRaw> {

    override suspend fun core2Remote(model: UserGroup): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerId = model.ownerId,
            data = crypto.encryptModel(groupId = model.id, data = model) ?: ""
        )
    }

    override suspend fun remote2Core(model: GroupDocumentRaw): UserGroup {
        return crypto.decryptModel<UserGroup>(
            groupId = model.id,
            data = model.data,
            senderId = model.ownerId
        )!!.copy(id = model.id)
    }

    override suspend fun local2Remote(model: UserGroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerId = model.ownerId,
            data = model.data
        )
    }

    override suspend fun remote2Local(model: GroupDocumentRaw): UserGroupEntity {
        return UserGroupEntity(
            id = model.id,
            ownerId = model.ownerId,
            data = model.data
        )
    }
}