package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.mappers.MemberMapperLocal
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class LocationLocalDataSource(
    private val dao: MemberDao,
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

    override fun getMemberModelsForUser(userId: String): Flow<MemberDocumentRaw> {
        return dao.getLastLocationForUser(userId).mapLatest(mapper::local2Remote)
    }

    override fun getMemberModelsForUsers(userIds: List<String>): Flow<List<MemberDocumentRaw>> {
        return mapper.mapFlowOfList(dao.getLocationsForUsers(userIds))
    }

    override fun getMembersForGroup(groupId: String): Flow<List<MemberDocumentRaw>> {
        return mapper.mapFlowOfList(dao.getLocationsForGroup(groupId))
    }

    override fun memberCountInGroup(groupId: String): Flow<Int> {
        return dao.memberCountInGroup(groupId)
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