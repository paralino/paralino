package app.paralino.android.data.repos.location

import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.extensions.distinctFlowOfList
import app.paralino.android.data.mappers.MemberMapperLocal
import app.paralino.android.data.models.location.MemberMutableDataUpdate
import app.paralino.android.data.repos.group.UserGroupDao
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

    override fun getMemberModelsForUser(userId: String): Flow<List<MemberModel>> {
        return getMemberModelsForUsers(listOf(userId))
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

    override fun getMemberCountInGroup(groupId: String): Flow<Int> {
        return dao.getMemberCountInGroup(groupId)
    }

    override suspend fun getMemberCountInGroupOnce(groupId: String): Int {
        return dao.getMemberCountInGroupOnce(groupId)
    }

    override suspend fun getUserMemberIds(userId: String, groupId: String): List<String> {
        return dao.getUserMemberIds(userId, groupId)
    }

    override suspend fun removeMember(id: String) {
        dao.removeMember(id)
    }

    override suspend fun clearMemberLocationData(memberId: String) {
        dao.updateMemberMutableData(MemberMutableDataUpdate(memberId, mutableData = null))
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