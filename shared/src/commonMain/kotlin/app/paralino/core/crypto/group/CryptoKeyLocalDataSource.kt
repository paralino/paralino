package app.paralino.core.crypto.group

import app.paralino.core.models.mappers.GroupKeysMapper
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CryptoKeyLocalDataSource constructor(
    private val database: AppDatabaseHelper,
    private val keysMapper: GroupKeysMapper,
) : ICryptoKeyLocalDataSource {

    override suspend fun recreateAllGroupKeys(
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
    ) {
        database.transaction {
            database.groupKeysQueries.clearAll()
            database.memberKeysQueries.clearAll()
            groupKeys.forEach {
                database.groupKeysQueries.createGroupKey(keysMapper.remote2Local(it))
            }
            memberKeys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
        }
    }

    override suspend fun recreateAllGroupKeys(keys: List<GroupCryptoKeyDataRaw>) {
        database.groupKeysQueries.transaction {
            database.groupKeysQueries.clearAll()
            keys.forEach {
                database.groupKeysQueries.createGroupKey(keysMapper.remote2Local(it))
            }
        }
    }

    override suspend fun recreateAllMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>) {
        database.memberKeysQueries.transaction {
            database.memberKeysQueries.clearAll()
            keys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
        }
    }

    override suspend fun recreateGroupKeyForGroup(
        groupId: String,
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>
    ) {
        database.transaction {
            database.groupKeysQueries.deleteForGroup(groupId)
            database.memberKeysQueries.deleteForGroup(groupId)
            groupKeys.forEach {
                database.groupKeysQueries.createGroupKey(keysMapper.remote2Local(it))
            }
            memberKeys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
        }
    }

    override suspend fun recreateGroupKeyForGroup(
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

    override suspend fun recreateMemberKeysForGroup(
        groupId: String,
        keys: List<GroupMemberCryptoKeyDataRaw>,
    ) {
        database.transaction {
            database.memberKeysQueries.deleteForGroup(groupId)
            keys.forEach {
                database.memberKeysQueries.createGroupMemberKey(keysMapper.remote2Local(it))
            }
        }
    }

    override fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return database.mapToList { groupKeysQueries.getAll() }
//            .distinctFlowOfList()
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    override fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return database.mapToList { memberKeysQueries.getAll() }
//            .distinctFlowOfList()
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    override suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupQueries.getGroupOwnerId(groupId).executeAsOneOrNull()
    }

    override suspend fun deleteAllKeysForGroup(groupId: String) {
        database.transaction {
            database.groupKeysQueries.deleteForGroup(groupId)
            database.memberKeysQueries.deleteForGroup(groupId)
        }
    }

    override suspend fun deleteKeysForUser(groupId: String, userId: String) {
        database.memberKeysQueries.transaction {
            database.memberKeysQueries.deleteForMember(groupId = groupId, userId = userId)
        }
    }

    override suspend fun deleteAllKeys() {
        database.transaction {
            database.groupKeysQueries.clearAll()
            database.memberKeysQueries.clearAll()
        }
    }
}