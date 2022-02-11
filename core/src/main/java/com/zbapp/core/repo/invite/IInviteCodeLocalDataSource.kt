package com.zbapp.core.repo.invite

import com.zbapp.core.models.invite.PersonalCodeData
import kotlinx.coroutines.flow.Flow

interface IInviteCodeLocalDataSource {
    suspend fun replaceInviteCode(data: List<PersonalCodeData>)
    suspend fun addInviteCode(data: PersonalCodeData)
    fun getAllInviteCodes(): Flow<List<PersonalCodeData>>
    fun getInviteCodesForUser(ownerId: String): Flow<List<PersonalCodeData>>
    suspend fun deleteInviteCode(id: String)
}