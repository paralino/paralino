package app.paralino.android.data.repos.location

import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.utils.exceptions.MissingGroupKeyException
import app.paralino.core.utils.extensions.distinctFlowOfList
import app.paralino.android.data.mappers.MemberWithLocationsMapperLocal
import app.paralino.android.data.models.location.MemberMutableDataUpdate
import app.paralino.android.data.repos.group.UserGroupDao
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.MemberWithLocationsRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class LocationLocalDataSource(
    private val dao: MemberDao,
    private val groupDao: UserGroupDao,
    private val mapper: MemberWithLocationsMapperLocal
) : ILocationLocalDataSource {

    override suspend fun createOrUpdateMembers(vararg data: MemberDocumentRaw) {
        dao.createOrUpdateMemberOnly(*data.map { mapper.memberMapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun recreateAllMember(data: List<MemberWithLocationsRaw>) {
        dao.recreateAll(data.map { mapper.remote2Local(it) })
    }

    override suspend fun recreateMembersForGroup(groupId: String, data: List<MemberWithLocationsRaw>) {
        dao.recreateForGroup(groupId, data.map { mapper.remote2Local(it) })
    }

    override fun getMemberRawPresentation(memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>> {
        return dao.getMember(memberId)
            .filterNotNull()
            .map {
                val key = groupDao.getGroupPublicKeyOnce(it.member.groupId)
                    ?: throw MissingGroupKeyException(it.member.groupId)
                mapper.local2Remote(it) to mapper.local2Core(it, key)
            }
    }

    override fun getMemberModelsForUser(userId: String): Flow<List<MemberModelWithLocations>> {
        return dao.getLocationsForUser(userId)
            .distinctFlowOfList()
            .map {
                it.map {
                    val key = groupDao.getGroupPublicKeyOnce(it.member.groupId)
                        ?: throw MissingGroupKeyException(it.member.groupId)
                    mapper.local2Core(it, key)
                }
            }
    }

    override fun getMembersForGroup(groupId: String): Flow<List<MemberModelWithLocations>> {
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
        return dao.getMemberCountInGroup(groupId).distinctUntilChanged()
    }

    override suspend fun getMemberCountInGroupOnce(groupId: String): Int {
        return dao.getMemberCountInGroupOnce(groupId)
    }

    override suspend fun getUserLocationsIds(groupId: String, userId: String): List<String> {
        return dao.getUserLocationIds(groupId = groupId, userId = userId)
    }

    override suspend fun getUserMemberId(groupId: String, userId: String): String? {
        return dao.getUserMemberId(groupId = groupId, userId = userId)
    }

    override suspend fun clearMemberLocationData(locationId: String) {
        dao.updateMemberMutableData(MemberMutableDataUpdate(id = locationId, locationData = null))
    }

    override suspend fun removeAllMembersForGroup(groupId: String) {
        dao.clearAllMembersAndLocationsForGroup(groupId)
    }

    override suspend fun removeMemberFromGroup(groupId: String, memberId: String) {
        dao.removeMemberWithLocationsForGroup(groupId, memberId)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}