package app.paralino.core.crypto.group

import app.paralino.core.appPrefs
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.keys.LocalGroupDeviceKeysEntity
import app.paralino.core.db.main.keys.LocalLocationHistoryKeyEntity
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow

class CryptoKeyLocalDataSource constructor(
    private val database: MainDatabaseHelper,
    private val keysMapper: GroupKeysMapper,
    private val appPrefs: AppPrefs
) {

    private var cachedGroupKeys = MutableStateFlow<List<GroupCryptoKeyDataRaw>>(emptyList())
    private var cachedMemberKeys = MutableStateFlow<List<GroupMemberCryptoKeyDataRaw>>(emptyList())
    private var cachedGroupDeviceKeys = MutableStateFlow<List<GroupDeviceCryptoKeyRaw>>(emptyList())
    private var cachedPersonalGroupKeys = MutableStateFlow<List<PersonalGroupKeyRaw>>(emptyList())

    suspend fun getGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return cachedGroupKeys.value.ifEmpty {
            getAllGroupKeys()
                .also { cachedGroupKeys.value = it }
        }
    }

    suspend fun getMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return cachedMemberKeys.value.ifEmpty {
            getAllMemberKeys()
                .also { cachedMemberKeys.value = it }
        }
    }

    suspend fun getGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return cachedGroupDeviceKeys.value.ifEmpty {
            getAllGroupDeviceKeys()
                .also { cachedGroupDeviceKeys.value = it }
        }
    }

    suspend fun getPersonalGroupKeys(): List<PersonalGroupKeyRaw> {
        return cachedPersonalGroupKeys.value.ifEmpty {
            getAllPersonalGroupKeys()
                .also { cachedPersonalGroupKeys.value = it }
        }
    }

    suspend fun getLocalGroupSenderKey(groupId: String): LocalGroupDeviceKeysEntity? {
        return database.groupRelatedKeysDao.getLocalGroupDeviceSenderKey(groupId = groupId)
    }

    suspend fun saveLocalGroupSenderKey(key: LocalGroupDeviceKeysEntity) {
        database.groupRelatedKeysDao.createLocalGroupDeviceSenderKey(key)
    }

    suspend fun getCurrentDeviceLocationId(groupId: String): String? {
        return database.groupRelatedDao.getSingleLocationForMemberAndDeviceOnce(
            groupId = groupId,
            userId = appPrefs.userId,
            deviceId = appPrefs.deviceId
        )?.id
    }

    suspend fun recreateAllGroupKeys(
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        deviceKeys: List<GroupDeviceCryptoKeyRaw>,
        personalKeys: List<PersonalGroupKeyRaw>
    ) {
        cachedGroupKeys.value = groupKeys
        cachedMemberKeys.value = memberKeys
        cachedGroupDeviceKeys.value = deviceKeys
        cachedPersonalGroupKeys.value = personalKeys

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
        cachedGroupKeys.value = emptyList()
        cachedMemberKeys.value = emptyList()
        cachedGroupDeviceKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()

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
        cachedGroupKeys.value = emptyList()
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
        cachedMemberKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()

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
        cachedGroupDeviceKeys.value = emptyList()
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
        safetySignature: String,
        userKeyVersion: Long
    ) {
        database.groupRelatedKeysDao.createGroupDeviceIntegrityKey(
            GroupDeviceIntegrityEntity(
                id = groupId,
                integrity = integrity,
                safetySignature = safetySignature,
                userKeyVersion = userKeyVersion
            )
        )
    }

    suspend fun saveGroupHistoryKey(
        groupId: String,
        userKeyVersion: Long,
        historyKeyVersion: String,
        encryptedHistoryKey: String,
        isSynced: Boolean,
    ) {
        database.locationHistoryDao.saveLocalHistoryKey(
            key = LocalLocationHistoryKeyEntity(
                groupId = groupId,
                createdAt = nowUTC().toString(),
                userKeyVersion = userKeyVersion,
                historyKeyVersion = historyKeyVersion,
                encryptedHistoryKey = encryptedHistoryKey,
                isSynced = isSynced
            )
        )
    }

    private suspend fun getAllGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return database.groupRelatedKeysDao.getAllGroupKeys()
            .map { keysMapper.local2Remote(it) }
    }

    private suspend fun getAllMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return database.groupRelatedKeysDao.getAllGroupMembersKeys()
            .map { keysMapper.local2Remote(it) }
    }

    private suspend fun getAllGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return database.groupRelatedKeysDao.getAllGroupDeviceKeys()
            .map { keysMapper.local2Remote(it) }
    }

    private suspend fun getAllPersonalGroupKeys(): List<PersonalGroupKeyRaw> {
        return database.groupRelatedKeysDao.getAllPersonalGroupKeys()
            .map { keysMapper.local2Remote(it) }
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupRelatedDao.getGroupOwnerUserId(groupId)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        cachedGroupKeys.value = cachedGroupKeys.value.filter { it.groupId != groupId }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.groupId != groupId }
        cachedGroupDeviceKeys.value = cachedGroupDeviceKeys.value.filter { it.groupId != groupId }
        cachedPersonalGroupKeys.value = cachedPersonalGroupKeys.value.filter { it.groupId != groupId }

        database.groupRelatedKeysDao.clearAllGroupRelatedKeys(groupId = groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        cachedGroupDeviceKeys.value = cachedGroupDeviceKeys.value.filter {
            // remove all keys that were either created by or for userId
            !(it.memberUserId == userId || it.ownerUserId == userId)
        }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.ownerUserId != userId }

        database.groupRelatedKeysDao.clearAllKeysForUser(groupId = groupId, userId = userId)
    }

    suspend fun deleteAllKeys() {
        cachedMemberKeys.value = emptyList()
        cachedGroupKeys.value = emptyList()
        cachedGroupDeviceKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()

        database.groupRelatedKeysDao.clearAllGroupRelatedKeys()
    }
}