package app.paralino.core.models.mappers

import app.paralino.core.data.db.GroupKeysEntity
import app.paralino.core.data.db.GroupMembersKeysEntity
import app.paralino.core.data.db.PersonalGroupKeyEntity
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw

class GroupKeysMapper {
    fun local2Remote(model: GroupKeysEntity): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            version = model.version,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

    fun remote2Local(model: GroupCryptoKeyDataRaw): GroupKeysEntity {
        return GroupKeysEntity(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            version = model.version,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

    fun local2Remote(model: GroupMembersKeysEntity): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            version = model.version,
            encryptedSenderKey = model.encryptedSenderKey,
            encryptedPublicKey = model.encryptedPublicKey,
            ownerIdentityKey = model.ownerIdentityKey
        )
    }

    fun remote2Local(model: GroupMemberCryptoKeyDataRaw): GroupMembersKeysEntity {
        return GroupMembersKeysEntity(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            version = model.version,
            encryptedSenderKey = model.encryptedSenderKey,
            encryptedPublicKey = model.encryptedPublicKey,
            ownerIdentityKey = model.ownerIdentityKey
        )
    }

    fun local2Remote(model: PersonalGroupKeyEntity): PersonalGroupKeyRaw {
        return PersonalGroupKeyRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedUserKeys = model.encryptedUserKeys,
            groupId = model.groupId,
            version = model.version
        )
    }

    fun remote2Local(model: PersonalGroupKeyRaw): PersonalGroupKeyEntity {
        return PersonalGroupKeyEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedUserKeys = model.encryptedUserKeys,
            groupId = model.groupId,
            version = model.version
        )
    }

}