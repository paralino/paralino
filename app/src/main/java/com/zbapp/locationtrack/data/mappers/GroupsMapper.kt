package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupData
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.mappers.MemberMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.base.IGroupCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

class GroupsMapper(
    private val crypto: IGroupCrypto,
    private val memberMapperLocal: MemberMapperLocal,
    private val memberMapperCore: MemberMapperCore
) : IGroupDataMapperCore, IDataMapperLocal<UserGroupEntity, GroupDocumentRaw> {

    override suspend fun core2Remote(model: UserGroup): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            data = crypto.encryptModel(groupId = model.id, data = model.data) ?: ""
        )
    }

    override suspend fun remote2Core(model: GroupDocumentRaw): UserGroup {
        val groupData = crypto.decryptModel<UserGroupData>(
            groupId = model.id,
            data = model.data,
            senderId = model.ownerUserId
        )!!

        return UserGroup(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            data = groupData,
            members = emptyList(),
            groupKey = ""
        )
    }

    override suspend fun local2Remote(model: UserGroupEntity): GroupDocumentRaw {
        return GroupDocumentRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            data = model.data
        )
    }

    override suspend fun remote2Local(model: GroupDocumentRaw): UserGroupEntity {
        return UserGroupEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            ownerPublicKey = model.ownerPublicKey,
            data = model.data
        )
    }
}