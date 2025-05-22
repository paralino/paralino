package app.paralino.core.models.mappers

import app.paralino.core.db.main.keys.GroupDeviceKeysEntity
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.GroupKeysEntity
import app.paralino.core.db.main.keys.GroupMembersKeysEntity
import app.paralino.core.db.main.keys.PersonalGroupKeyEntity
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupLocationHistoryCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw

class GroupKeysMapper {
    fun local2Remote(model: GroupKeysEntity): GroupCryptoKeyDataRaw {
        return GroupCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

    fun remote2Local(model: GroupCryptoKeyDataRaw): GroupKeysEntity {
        return GroupKeysEntity(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            groupKeyVersion = model.groupKeyVersion,
            memberKeyVersion = model.memberKeyVersion,
            encryptedGroupKey = model.encryptedGroupKey
        )
    }

    fun local2Remote(model: GroupMembersKeysEntity): GroupMemberCryptoKeyDataRaw {
        return GroupMemberCryptoKeyDataRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            memberKeyVersion = model.memberKeyVersion,
            signedMemberKxPublicKey = model.signedMemberKxPublicKey,
            signedMembershipPublicKey = model.signedMembershipPublicKey,
            ownerIdentityKey = model.ownerIdentityKey
        )
    }

    fun remote2Local(model: GroupMemberCryptoKeyDataRaw): GroupMembersKeysEntity {
        return GroupMembersKeysEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            memberKeyVersion = model.memberKeyVersion,
            signedMemberKxPublicKey = model.signedMemberKxPublicKey,
            signedMembershipPublicKey = model.signedMembershipPublicKey,
            ownerIdentityKey = model.ownerIdentityKey
        )
    }

    fun local2Remote(model: GroupDeviceKeysEntity): GroupDeviceCryptoKeyRaw {
        return GroupDeviceCryptoKeyRaw(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            locationId = model.locationId,
            memberKeyVersion = model.memberKeyVersion,
            deviceKeyVersion = model.deviceKeyVersion,
            encryptedSenderKey = model.encryptedSenderKey,
        )
    }

    fun remote2Local(model: GroupDeviceCryptoKeyRaw): GroupDeviceKeysEntity {
        return GroupDeviceKeysEntity(
            id = model.id,
            groupId = model.groupId,
            memberUserId = model.memberUserId,
            ownerUserId = model.ownerUserId,
            locationId = model.locationId,
            memberKeyVersion = model.memberKeyVersion,
            deviceKeyVersion = model.deviceKeyVersion,
            encryptedSenderKey = model.encryptedSenderKey,
        )
    }

    fun local2Remote(model: PersonalGroupKeyEntity): PersonalGroupKeyRaw {
        return PersonalGroupKeyRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedUserKeys = model.encryptedUserKeys,
            groupId = model.groupId,
            personalGroupKeyVersion = model.personalGroupKeyVersion,
            userKeyVersion = model.userKeyVersion
        )
    }

    fun remote2Local(model: PersonalGroupKeyRaw): PersonalGroupKeyEntity {
        return PersonalGroupKeyEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedUserKeys = model.encryptedUserKeys,
            groupId = model.groupId,
            personalGroupKeyVersion = model.personalGroupKeyVersion,
            userKeyVersion = model.userKeyVersion
        )
    }

    fun local2Remote(model: GroupHistoryKeyEntity): GroupLocationHistoryCryptoKeyRaw {
        return GroupLocationHistoryCryptoKeyRaw(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            memberUserId = model.memberUserId,
            memberKeyVersion = model.memberKeyVersion,
            historyKeyVersion = model.historyKeyVersion,
            encryptedHistoryKey = model.encryptedHistoryKey
        )
    }

    fun remote2Local(model: GroupLocationHistoryCryptoKeyRaw): GroupHistoryKeyEntity {
        return GroupHistoryKeyEntity(
            id = model.id,
            groupId = model.groupId,
            ownerUserId = model.ownerUserId,
            memberUserId = model.memberUserId,
            memberKeyVersion = model.memberKeyVersion,
            historyKeyVersion = model.historyKeyVersion,
            encryptedHistoryKey = model.encryptedHistoryKey
        )
    }
}