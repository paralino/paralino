package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.base.IGroupCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

class GroupsMapper(
    private val crypto: IGroupCrypto,
    private val memberMapper: MemberMapper
) : IGroupDataMapperCore, IDataMapperLocal<UserGroupEntity, GroupDocumentRaw> {

    override suspend fun core2Remote(model: UserGroup): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerId = model.ownerId,
            data = crypto.encryptModel(groupId = model.id, data = model.copy(members = emptyList())) ?: "",
            members = model.members.map { memberMapper.core2Remote(it) }
        )
    }

    override suspend fun remote2Core(model: GroupDocumentRaw): UserGroup {
        val members = model.members.map { memberMapper.remote2Core(it) }
        return crypto.decryptModel<UserGroup>(
            groupId = model.id,
            data = model.data,
            senderId = model.ownerId
        )!!.copy(id = model.id, members = members)
    }

    override suspend fun local2Remote(model: UserGroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerId = model.ownerId,
            data = model.data,
            members = model.members.map { memberMapper.local2Remote(it) }
        )
    }

    override suspend fun remote2Local(model: GroupDocumentRaw): UserGroupEntity {
        return UserGroupEntity(
            id = model.id,
            ownerId = model.ownerId,
            data = model.data,
            members = model.members.map { memberMapper.remote2Local(it) }
        )
    }
}