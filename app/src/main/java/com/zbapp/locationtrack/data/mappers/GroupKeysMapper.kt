package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.mappers.IDataMapperLocal
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.locationtrack.data.models.group.GroupKeyEntity

class GroupKeysMapper: IDataMapperLocal<GroupKeyEntity, GroupCryptoKeyDataRaw> {
    override suspend fun local2Remote(model: GroupKeyEntity): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            version = model.version,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

    override suspend fun remote2Local(model: GroupCryptoKeyDataRaw): GroupKeyEntity {
        return GroupKeyEntity(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            version = model.version,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

}