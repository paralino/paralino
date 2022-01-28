package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.locationtrack.data.models.group.UserGroupEntity

object GroupsMapperLocal : IDataMapperLocal<UserGroupEntity, GroupDocumentRaw> {

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