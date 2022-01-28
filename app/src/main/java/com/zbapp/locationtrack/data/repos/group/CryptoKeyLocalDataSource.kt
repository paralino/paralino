package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.crypto.ICryptoKeyLocalDataSource
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.locationtrack.data.mappers.GroupKeysMapper
import com.zbapp.locationtrack.data.mappers.GroupMemberKeysMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow

class CryptoKeyLocalDataSource constructor(
    private val keysDao: GroupKeysDao,
    private val memberKeysDao: GroupMemberKeysDao,
    private val keysMapper: GroupKeysMapper,
    private val keysMemberMapper: GroupMemberKeysMapper
): ICryptoKeyLocalDataSource {

    override suspend fun recreateAllGroupKeys(keys: List<GroupCryptoKeyDataRaw>) {
        keysDao.recreateAll(keys.map { keysMapper.remote2Local(it) })
    }

    override suspend fun recreateAllMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>) {
        memberKeysDao.recreateAll(keys.map { keysMemberMapper.remote2Local(it) })
    }

    override suspend fun recreateGroupKeyForGroup(groupId: String, key: GroupCryptoKeyDataRaw) {
        keysDao.deleteForGroup(groupId)
        keysDao.createOrUpdate(keysMapper.remote2Local(key))
    }

    override suspend fun recreateMemberKeysForGroup(
        groupId: String,
        keys: List<GroupMemberCryptoKeyDataRaw>
    ) {
        memberKeysDao.deleteForGroup(groupId)
        memberKeysDao.createOrUpdate(*keys.map { keysMemberMapper.remote2Local(it) }.toTypedArray())
    }

    override fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return keysMapper.mapFlowOfList(keysDao.getAll())
    }

    override fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return keysMemberMapper.mapFlowOfList(memberKeysDao.getAll())
    }

    override suspend fun deleteGroupKeyForGroup(groupId: String) {
        keysDao.deleteForGroup(groupId)
    }

    override suspend fun deleteAllKeysForGroup(groupId: String) {
        keysDao.deleteForGroup(groupId)
        memberKeysDao.deleteForGroup(groupId)
    }

    override suspend fun deleteKeysForUser(groupId: String, userId: String) {
        memberKeysDao.deleteUserFromGroup(groupId, userId)
    }

    override suspend fun deleteAllKeys() {
        keysDao.clear()
        memberKeysDao.clear()
    }
}