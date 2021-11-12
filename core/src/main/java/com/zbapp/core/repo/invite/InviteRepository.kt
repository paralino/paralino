package com.zbapp.core.repo.invite

import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import kotlinx.coroutines.flow.Flow

class InviteRepository(
    private val api: IInviteApi
) : BaseRepository() {

    fun createInviteCode(data: InviteCode): Flow<InviteCode> {
        return getDataOnce(
            remote = { api.createInviteCode(data) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun getMyInviteCodes(): Flow<List<InviteCode>> {
        return getDataOnce(
            remote = { api.getMyInviteCodes() },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun getInviteByCode(code: String): Flow<InviteCode> {
        return getDataOnce(
            remote = { api.getInviteByCode(code) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun getInviteByUrl(url: String): Flow<InviteCode> {
        return getDataOnce(
            remote = { api.getInviteByUrl(url) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun revokeInviteCode(id: String): Flow<Unit> {
        return getDataOnce(
            remote = { api.revokeInviteCode(id) },
            remotePreference = RemotePreference.FORCE
        )
    }
}