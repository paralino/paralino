package app.paralino.core.crypto.group

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CryptoKeyLocalDataSource constructor(
    private val database: AppDatabaseHelper,
    private val keysMapper: GroupKeysMapper,
) {

    suspend fun recreateAllGroupKeys(
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        personalKeys: List<PersonalGroupKeyRaw>
    ) {
        database.transaction {
            database.groupKeysQueries.clearAll()
            database.memberKeysQueries.clearAll()
            database.personalGroupKeysQueries.clearAll()
            groupKeys.forEach {
                database.groupKeysQueries.createGroupKey(keysMapper.remote2Local(it))
            }
            memberKeys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
            personalKeys.forEach {
                database.personalGroupKeysQueries.createKey(keysMapper.remote2Local(it))
            }
        }
    }

    suspend fun recreateGroupKeyForGroup(
        groupId: String,
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        preKeys: List<PersonalGroupKeyRaw>
    ) {
        database.transaction {
            database.groupKeysQueries.deleteForGroup(groupId)
            database.memberKeysQueries.deleteForGroup(groupId)
            database.personalGroupKeysQueries.deleteForGroup(groupId)
            groupKeys.forEach {
                database.groupKeysQueries.createGroupKey(keysMapper.remote2Local(it))
            }
            memberKeys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
            preKeys.forEach {
                database.personalGroupKeysQueries.createKey(keysMapper.remote2Local(it))
            }
        }
    }

    suspend fun recreateGroupKeyForGroup(
        groupId: String,
        keys: List<GroupCryptoKeyDataRaw>,
    ) {
        database.groupKeysQueries.transaction {
            database.groupKeysQueries.deleteForGroup(groupId)
            keys.forEach {
                database.groupKeysQueries.createGroupKey(keysMapper.remote2Local(it))
            }
        }
    }

    suspend fun recreateMemberKeysForGroup(
        groupId: String,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
        preKeys: List<PersonalGroupKeyRaw>,
    ) {
        database.transaction {
            database.memberKeysQueries.deleteForGroup(groupId)
            database.personalGroupKeysQueries.deleteForGroup(groupId)
            memberKeys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
            preKeys.forEach {
                database.personalGroupKeysQueries.createKey(keysMapper.remote2Local(it))
            }
        }
    }

    fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return database.mapToList { groupKeysQueries.getAll() }
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return database.mapToList { memberKeysQueries.getAll() }
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    fun getAllPersonalGroupKeys(): Flow<List<PersonalGroupKeyRaw>> {
        return database.mapToList { personalGroupKeysQueries.getAllKeys() }
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupQueries.getGroupOwnerId(groupId).executeAsOneOrNull()
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        database.transaction {
            database.groupKeysQueries.deleteForGroup(groupId)
            database.memberKeysQueries.deleteForGroup(groupId)
            database.personalGroupKeysQueries.deleteForGroup(groupId)
        }
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        database.memberKeysQueries.transaction {
            database.memberKeysQueries.deleteForMember(groupId = groupId, userId = userId)
        }
    }

    suspend fun deleteAllKeys() {
        database.transaction {
            database.groupKeysQueries.clearAll()
            database.memberKeysQueries.clearAll()
            database.personalGroupKeysQueries.clearAll()
        }
    }
}