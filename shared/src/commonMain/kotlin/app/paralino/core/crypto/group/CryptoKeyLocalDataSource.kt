package app.paralino.core.crypto.group

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw

class CryptoKeyLocalDataSource constructor(
    private val database: MainDatabaseHelper,
    private val keysMapper: GroupKeysMapper,
) {

    suspend fun recreateAllGroupKeys(
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
        personalKeys: List<PersonalGroupKeyRaw>
    ) {
        database.groupRelatedKeysDao.recreateAllKeys(
            groupKeys = groupKeys.map { keysMapper.remote2Local(it) },
            memberKeys = memberKeys.map { keysMapper.remote2Local(it) },
            deviceKeys = deviceKeys.map { keysMapper.remote2Local(it) },
            personalKeys = personalKeys.map { keysMapper.remote2Local(it) }
        )
    }

    suspend fun recreateGroupKeyForGroup(
        groupId: String,
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
        personalKeys: List<PersonalGroupKeyRaw>
    ) {
        database.groupRelatedKeysDao.recreateAllKeys(
            groupId = groupId,
            groupKeys = groupKeys.map { keysMapper.remote2Local(it) },
            memberKeys = memberKeys.map { keysMapper.remote2Local(it) },
            deviceKeys = deviceKeys.map { keysMapper.remote2Local(it) },
            personalKeys = personalKeys.map { keysMapper.remote2Local(it) }
        )
    }

    suspend fun recreateGroupKeyForGroup(
        groupId: String,
        keys: List<GroupCryptoKeyDataRaw>,
    ) {
        database.groupRelatedKeysDao.recreateGroupKeys(
            groupId = groupId,
            groupKeys = keys.map { keysMapper.remote2Local(it) }
        )
    }

    suspend fun recreateMemberKeysForGroup(
        groupId: String,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        personalKeys: List<PersonalGroupKeyRaw>,
    ) {
        database.groupRelatedKeysDao.recreateMemberKeys(
            groupId = groupId,
            memberKeys = memberKeys.map { keysMapper.remote2Local(it) },
            personalKeys = personalKeys.map { keysMapper.remote2Local(it) }
        )
    }

    suspend fun recreateDeviceKeysForGroup(
        groupId: String,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
    ) {
        database.groupRelatedKeysDao.recreateDeviceKeys(
            groupId = groupId,
            deviceKeys = deviceKeys.map { keysMapper.remote2Local(it) }
        )
    }

    suspend fun getGroupDeviceIntegrity(groupId: String): GroupDeviceIntegrityEntity? {
        return database.groupRelatedKeysDao.getGroupDeviceIntegrityKey(groupId)
    }

    suspend fun updateGroupDeviceIntegrity(
        groupId: String,
        integrity: String,
        userKeyVersion: Long
    ) {
        database.groupRelatedKeysDao.createGroupDeviceIntegrityKey(
            GroupDeviceIntegrityEntity(
                id = groupId,
                integrity = integrity,
                userKeyVersion = userKeyVersion
            )
        )
    }

    suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return database.groupRelatedKeysDao.getAllGroupKeys()
            .map { keysMapper.local2Remote(it) }
    }

    suspend fun getAllMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return database.groupRelatedKeysDao.getAllGroupMembersKeys()
            .map { keysMapper.local2Remote(it) }
    }

    suspend fun getAllGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return database.groupRelatedKeysDao.getAllGroupDeviceKeys()
            .map { keysMapper.local2Remote(it) }
    }

    suspend fun getAllPersonalGroupKeys(): List<PersonalGroupKeyRaw> {
        return database.groupRelatedKeysDao.getAllPersonalGroupKeys()
            .map { keysMapper.local2Remote(it) }
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupRelatedDao.getGroupOwnerUserId(groupId)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        database.groupRelatedKeysDao.clearAllGroupRelatedKeys(groupId = groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        database.groupRelatedKeysDao.clearAllKeysForUser(groupId = groupId, userId = userId)
    }

    suspend fun deleteAllKeys() {
        database.groupRelatedKeysDao.clearAllGroupRelatedKeys()
    }
}