package com.zbapp.core.repo.invite

import com.zbapp.core.config
import com.zbapp.core.models.invite.InviteCode
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import kotlinx.coroutines.flow.Flow

class InviteRepository(
    private val api: IInviteApi,
    private val local: IInviteCodeLocalDataSource
) : BaseRepository() {

    fun createInviteCode(data: InviteCode): Flow<InviteCode> {
        return getDataOnce(
            remote = { api.createInviteCode(data) },
            remotePreference = RemotePreference.FORCE,
            localUpdate = { local.addInviteCode(it) }
        )
    }

    fun getAllInviteCodes(): Flow<List<InviteCode>> {
        return subscribeToData(
            remote = { api.getMyInviteCodes() },
            local = local.getAllInviteCodes(),
            localUpdate = { local.replaceInviteCode(it) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun getMyInviteCodes(): Flow<List<InviteCode>> {
        return subscribeToData(
            remote = { api.getMyInviteCodes() },
            local = local.getInviteCodesForUser(config.userId),
            localUpdate = { local.replaceInviteCode(it) },
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
            localUpdate = { local.deleteInviteCode(id) },
            remotePreference = RemotePreference.FORCE
        )
    }
}