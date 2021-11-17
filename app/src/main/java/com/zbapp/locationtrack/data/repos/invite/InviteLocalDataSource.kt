package com.zbapp.locationtrack.data.repos.invite

import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.invite.IInviteCodeLocalDataSource
import com.zbapp.locationtrack.data.mappers.InviteCodeMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow

class InviteLocalDataSource(
    private val dao: InviteCodeDao,
    private val mapper: InviteCodeMapper
) : IInviteCodeLocalDataSource {

    override suspend fun replaceInviteCode(data: List<InviteCode>) {
        dao.replaceAll(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun addInviteCode(data: InviteCode) {
        dao.createOrUpdate(mapper.remote2Local(data))
    }

    override fun getAllInviteCodes(): Flow<List<InviteCode>> {
        return mapper.mapFlowOfList(dao.getAll())
    }

    override fun getInviteCodesForUser(ownerId: String): Flow<List<InviteCode>> {
        return mapper.mapFlowOfList(dao.getForOwner(ownerId))
    }

    override suspend fun deleteInviteCode(id: String) {
        dao.delete(id)
    }
}