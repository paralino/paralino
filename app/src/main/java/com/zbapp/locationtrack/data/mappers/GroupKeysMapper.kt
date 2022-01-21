package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.locationtrack.data.models.group.GroupKeyEntity

class GroupKeysMapper: IDataMapperLocal<GroupKeyEntity, GroupCryptoKeyDataRaw> {
    override suspend fun local2Remote(model: GroupKeyEntity): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            memberId = model.memberId,
            memberUserId = model.memberUserId,
            time = model.time,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

    override suspend fun remote2Local(model: GroupCryptoKeyDataRaw): GroupKeyEntity {
        return GroupKeyEntity(
            id = model.id,
            groupId = model.groupId,
            memberId = model.memberId,
            memberUserId = model.memberUserId,
            time = model.time,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

}