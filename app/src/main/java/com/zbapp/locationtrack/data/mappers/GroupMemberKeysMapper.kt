package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.locationtrack.data.models.group.GroupMemberKeyEntity

class GroupMemberKeysMapper: IDataMapperLocal<GroupMemberKeyEntity, GroupMemberCryptoKeyDataRaw> {
    override suspend fun local2Remote(model: GroupMemberKeyEntity): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            version = model.version,
            encryptedSenderKey = model.encryptedSenderKey
        )
    }

    override suspend fun remote2Local(model: GroupMemberCryptoKeyDataRaw): GroupMemberKeyEntity {
        return GroupMemberKeyEntity(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            version = model.version,
            encryptedSenderKey = model.encryptedSenderKey
        )
    }

}