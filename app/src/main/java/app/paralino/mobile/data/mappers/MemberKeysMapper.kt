package app.paralino.mobile.data.mappers

import app.paralino.core.models.mappers.IDataMapperLocal
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.mobile.data.models.group.GroupMemberKeyEntity

class MemberKeysMapper: IDataMapperLocal<GroupMemberKeyEntity, GroupMemberCryptoKeyDataRaw> {
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