package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.crypto.ICryptoKeyLocalDataSource
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.core.utils.distinctFlowOfList
import com.zbapp.locationtrack.data.mappers.GroupKeysMapper
import com.zbapp.locationtrack.data.mappers.GroupMemberKeysMapper
import com.zbapp.locationtrack.data.repos.location.MemberDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CryptoKeyLocalDataSource constructor(
    private val keysDao: GroupKeysDao,
    private val groupDao: UserGroupDao,
    private val membersDao: MemberDao,
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

    override suspend fun recreateGroupKeyForGroup(groupId: String, keys: List<GroupCryptoKeyDataRaw>) {
        keysDao.deleteForGroup(groupId)
        keysDao.createOrUpdate(*keys.map { keysMapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun recreateMemberKeysForGroup(
        groupId: String,
        keys: List<GroupMemberCryptoKeyDataRaw>
    ) {
        memberKeysDao.deleteForGroup(groupId)
        memberKeysDao.createOrUpdate(*keys.map { keysMemberMapper.remote2Local(it) }.toTypedArray())
    }

    override fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return keysDao.getAll()
            .distinctFlowOfList()
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    override fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return memberKeysDao.getAll()
            .distinctFlowOfList()
            .map { it.map { keysMemberMapper.local2Remote(it) } }
    }

    override suspend fun getGroupOwnerPublicKey(groupId: String): String? {
        return groupDao.getGroupPublicKeyOnce(groupId)
    }

    override suspend fun getMemberPublicKey(groupId: String, userId: String): String? {
        return membersDao.getMemberPublicKeyOnce(groupId, userId)
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