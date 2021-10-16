package com.zbapp.core.repo.invite

import com.zbapp.core.models.invite.InviteCode

interface IInviteApi {
    suspend fun createInviteCode(data: InviteCode): InviteCode
    suspend fun getMyInviteCodes(): List<InviteCode>
    suspend fun getInviteByCode(code: String): InviteCode
    suspend fun getInviteByUrl(url: String): InviteCode
    suspend fun revokeInviteCode(id: String)
}