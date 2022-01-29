package com.zbapp.locationtrack.data.repos.invite

import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.core.repo.invite.IInviteCodeLocalDataSource
import com.zbapp.core.utils.distinctFlowOfList
import com.zbapp.locationtrack.data.mappers.InviteCodeMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InviteLocalDataSource(
    private val dao: InviteCodeDao,
    private val mapper: InviteCodeMapper
) : IInviteCodeLocalDataSource {

    override suspend fun replaceInviteCode(data: List<PersonalCodeData>) {
        dao.replaceAll(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun addInviteCode(data: PersonalCodeData) {
        dao.createOrUpdate(mapper.remote2Local(data))
    }

    override fun getAllInviteCodes(): Flow<List<PersonalCodeData>> {
        return dao.getAll()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    override fun getInviteCodesForUser(ownerId: String): Flow<List<PersonalCodeData>> {
        return dao.getForOwner(ownerId)
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    override suspend fun deleteInviteCode(id: String) {
        dao.delete(id)
    }
}