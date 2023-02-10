package app.paralino.android.data.repos.group

import app.paralino.core.crypto.group.ICryptoKeyLocalDataSource
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.android.data.mappers.GroupKeysMapper
import app.paralino.android.data.mappers.MemberKeysMapper
import app.paralino.android.data.repos.location.MemberDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CryptoKeyLocalDataSource constructor(
    private val keysDao: GroupKeysDao,
    private val groupDao: UserGroupDao,
    private val membersDao: MemberDao,
    private val keysMapper: GroupKeysMapper,
    private val keysMemberMapper: MemberKeysMapper,
) : ICryptoKeyLocalDataSource {

    override suspend fun recreateAllGroupKeys(
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>,
    ) {
        keysDao.recreateAllKeys(
            groupKeys = groupKeys.map { keysMapper.remote2Local(it) },
            memberKeys = memberKeys.map { keysMemberMapper.remote2Local(it) }
        )
    }

    override suspend fun recreateAllGroupKeys(keys: List<GroupCryptoKeyDataRaw>) {
        keysDao.recreateAllGroupKeys(keys.map { keysMapper.remote2Local(it) })
    }

    override suspend fun recreateAllMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>) {
        keysDao.recreateAllMemberKeys(keys.map { keysMemberMapper.remote2Local(it) })
    }

    override suspend fun recreateGroupKeyForGroup(
        groupId: String,
        groupKeys: List<GroupCryptoKeyDataRaw>,
        memberKeys: List<GroupMemberCryptoKeyDataRaw>
    ) {
        keysDao.recreateAllGroupKeysForGroup(
            groupId = groupId,
            groupKeys = groupKeys.map { keysMapper.remote2Local(it) },
            memberKeys = memberKeys.map { keysMemberMapper.remote2Local(it) }
        )
    }

    override suspend fun recreateGroupKeyForGroup(
        groupId: String,
        keys: List<GroupCryptoKeyDataRaw>,
    ) {
        keysDao.recreateGroupKeysForGroup(groupId, keys.map { keysMapper.remote2Local(it) })
    }

    override suspend fun recreateMemberKeysForGroup(
        groupId: String,
        keys: List<GroupMemberCryptoKeyDataRaw>,
    ) {
        keysDao.recreateMemberKeysForGroup(groupId, keys.map { keysMemberMapper.remote2Local(it) })
    }

    override fun getAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return keysDao.getAllGroupKeys()
//            .distinctFlowOfList()
            .map { it.map { keysMapper.local2Remote(it) } }
    }

    override fun getAllMemberKeys(): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return keysDao.getAllMemberKeys()
//            .distinctFlowOfList()
            .map { it.map { keysMemberMapper.local2Remote(it) } }
    }

    override suspend fun getGroupOwnerId(groupId: String): String? {
        return groupDao.getGroupOwnerIdOnce(groupId)
    }

    override suspend fun deleteGroupKeyForGroup(groupId: String) {
        keysDao.deleteGroupKeysForGroup(groupId)
    }

    override suspend fun deleteAllKeysForGroup(groupId: String) {
        keysDao.deleteAllKeysForGroup(groupId)
    }

    override suspend fun deleteKeysForUser(groupId: String, userId: String) {
        keysDao.deleteUserFromGroup(groupId, userId)
    }

    override suspend fun deleteAllKeys() {
        keysDao.clear()
    }
}