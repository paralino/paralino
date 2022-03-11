package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.utils.exceptions.MissingGroupKeyException
import com.zbapp.core.utils.extensions.distinctFlowOfList
import com.zbapp.locationtrack.data.mappers.MemberMapperLocal
import com.zbapp.locationtrack.data.repos.group.UserGroupDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class LocationLocalDataSource(
    private val dao: MemberDao,
    private val groupDao: UserGroupDao,
    private val mapper: MemberMapperLocal
) : ILocationLocalDataSource {

    override suspend fun createOrUpdateMembers(vararg data: MemberDocumentRaw) {
        dao.createOrUpdate(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun recreateAllMember(data: List<MemberDocumentRaw>) {
        dao.recreateAll(data.map { mapper.remote2Local(it) })
    }

    override suspend fun recreateMembersForGroup(groupId: String, data: List<MemberDocumentRaw>) {
        dao.recreateForGroup(groupId, data.map { mapper.remote2Local(it) })
    }

    override fun getMemberModelsForUser(userId: String): Flow<MemberModel> {
        return dao.getLastLocationForUser(userId)
            .distinctUntilChanged()
            .map {
                val key = it.groupId?.let { groupDao.getGroupPublicKeyOnce(it) } ?: ""
                mapper.local2Core(it, key)
            }
    }

    override fun getMemberModelsForUsers(userIds: List<String>): Flow<List<MemberModel>> {
        return dao.getLocationsForUsers(userIds)
            .distinctFlowOfList()
            .map {
                it.map {
                    val key = groupDao.getGroupPublicKeyOnce(it.groupId)
                        ?: throw MissingGroupKeyException(it.groupId)
                    mapper.local2Core(it, key)
                }
            }
    }

    override fun getMembersForGroup(groupId: String): Flow<List<MemberModel>> {
        return dao.getLocationsForGroup(groupId)
            .distinctFlowOfList()
            .map {
                val key = groupDao.getGroupPublicKeyOnce(groupId)
                if(key == null && it.isNotEmpty())
                    throw MissingGroupKeyException(groupId)
                it.map {
                    mapper.local2Core(it, key!!)
                }
            }
    }

    override fun memberCountInGroup(groupId: String): Flow<Int> {
        return dao.memberCountInGroup(groupId)
    }

    override suspend fun getUserMemberIds(userId: String, groupId: String): List<String> {
        return dao.getUserMemberIds(userId, groupId)
    }

    override suspend fun removeMember(id: String) {
        dao.removeMember(id)
    }

    override suspend fun clearAllMembersForUser(userId: String) {
        dao.clearAllMembersForUser(userId)
    }

    override suspend fun clearAllMembersForGroup(groupId: String) {
        dao.clearAllMembersForGroup(groupId)
    }

    override suspend fun clearAllMembersForGroup(groupId: String, userId: String) {
        dao.clearAllMembersForGroup(groupId, userId)
    }

    override suspend fun clearAll() {
        dao.clear()
    }
}