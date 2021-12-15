package com.zbapp.core.repo.invite

import com.zbapp.core.models.invite.InviteCode
import kotlinx.coroutines.flow.Flow

interface IInviteCodeLocalDataSource {
    suspend fun replaceInviteCode(data: List<InviteCode>)
    suspend fun addInviteCode(data: InviteCode)
    fun getAllInviteCodes(): Flow<List<InviteCode>>
    fun getInviteCodesForUser(ownerId: String): Flow<List<InviteCode>>
    suspend fun deleteInviteCode(id: String)
}