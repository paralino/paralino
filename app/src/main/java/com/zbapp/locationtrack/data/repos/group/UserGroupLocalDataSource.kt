package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.mappers.decrypt
import com.zbapp.core.models.mappers.decryptList
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.mappers.GroupKeysMapper
import com.zbapp.locationtrack.data.mappers.GroupMemberKeysMapper
import com.zbapp.locationtrack.data.mappers.GroupsMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class UserGroupLocalDataSource(
    private val dao: UserGroupDao,
    private val keysDao: GroupKeysDao,
    private val memberKeysDao: GroupMemberKeysDao,
    private val mapper: GroupsMapper,
    private val keysMapper: GroupKeysMapper,
    private val keysMemberMapper: GroupMemberKeysMapper,
    private val memberLocal: ILocationLocalDataSource
) : IGroupLocalDataSource {

    override suspend fun recreateGroups(data: List<GroupWithMemberRaw>) {
        dao.replaceGroups(*data.map { mapper.remote2Local(it.group) }.toTypedArray())
        memberLocal.recreateAllMember(data.flatMap { it.members })
        recreateGroupKeys(data.map { it.groupKey })
        recreateGroupMemberKeys(data.flatMap { it.memberKeys })
    }

    override suspend fun updateOrCreateGroup(group: GroupWithMemberRaw) {
        dao.createOrUpdate(mapper.remote2Local(group.group))
        memberLocal.recreateMembersForGroup(group.group.id, group.members)
        keysDao.deleteForGroup(group.group.id)
        keysDao.createOrUpdate(keysMapper.remote2Local(group.groupKey))
        memberKeysDao.deleteForGroup(group.group.id)
        memberKeysDao.createOrUpdate(*group.memberKeys.map { keysMemberMapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(group))
    }

    override suspend fun recreateGroupKeys(keys: List<GroupCryptoKeyDataRaw>) {
        keysDao.recreateAll(keys.map { keysMapper.remote2Local(it) })
    }

    override suspend fun recreateGroupMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>) {
        memberKeysDao.recreateAll(keys.map { keysMemberMapper.remote2Local(it) })
    }

    override suspend fun saveGroupKeys(vararg keys: GroupCryptoKeyDataRaw) {
        keysDao.createOrUpdate(*keys.map { keysMapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun saveGroupMemberKeys(vararg keys: GroupMemberCryptoKeyDataRaw) {
        memberKeysDao.createOrUpdate(*keys.map { keysMemberMapper.remote2Local(it) }.toTypedArray())
    }

    override fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId)
            .map { mapper.local2Remote(it) }
            .decrypt(mapper)
    }

    override fun getGroupWithMembers(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId)
            .filterNotNull()
            .combine(memberLocal.getMembersForGroup(groupId)) { group, members ->
                // TODO add members
                mapper.local2Remote(group)
            }
            .decrypt(mapper)
    }

    override fun getAllGroups(): Flow<List<UserGroup>> {
        return mapper.mapFlowOfList(dao.getAllGroups())
            .decryptList(mapper)
            .map { it.sortedBy { it.name } }
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
        keysDao.deleteForGroup(groupId)
        memberKeysDao.deleteForGroup(groupId)
        memberLocal.clearAllMembersForGroup(groupId)
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        memberLocal.clearAllMembersForGroup(groupId, userId)
        keysDao.deleteUserFromGroup(groupId, userId)
        memberKeysDao.deleteUserFromGroup(groupId, userId)
    }

    override suspend fun clearAll() {
        dao.clear()
        memberLocal.clearAll()
    }

}