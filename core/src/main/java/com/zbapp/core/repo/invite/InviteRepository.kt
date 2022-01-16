package com.zbapp.core.repo.invite

import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.core.models.invite.PersonalCodeCreateData
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.mapToPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InviteRepository(
    private val remote: InviteRemoteSource,
    private val local: IInviteCodeLocalDataSource,
    private val userRepo: UserRepository
) : BaseRepository() {

    fun createInviteCode(): Flow<Payload<PersonalCodeData>> {
        val user = userRepo.cachedUser
            ?: return flow { emit(Payload.Error(Throwable("User not valid"))) }
        val invite = PersonalCodeCreateData(
            ownerId = user.id,
            ownerName = user.name,
            ownerImageBase64 = user.data?.imageBase64 ?: "",
            ownerKey = user.data?.latestAsymmetricKey?.publicKey!!
        )
        return getDataOnce(
            remote = { remote.createInviteCode(invite) },
            remotePreference = RemotePreference.FORCE,
            localUpdate = { local.addInviteCode(it) }
        ).mapToPayload()
    }

    fun getAllInviteCodes(): Flow<Payload<List<PersonalCodeData>>> {
        return subscribeToData(
            remote = { remote.getInviteCodes() },
            local = local.getAllInviteCodes(),
            localUpdate = { local.replaceInviteCode(it) },
            remotePreference = RemotePreference.FORCE
        ).mapToPayload()
    }

    fun getMyInviteCodes(): Flow<Payload<List<PersonalCodeData>>> {
        return subscribeToData(
            remote = { remote.getInviteCodes() },
            local = local.getInviteCodesForUser(config.userId),
            localUpdate = { local.replaceInviteCode(it) },
            remotePreference = RemotePreference.FORCE
        ).mapToPayload()
    }

    fun getInviteByCode(code: String): Flow<Payload<PersonalCodeReadData>> {
        return getDataOnce(
            remote = { remote.getInviteByCode(code) },
            remotePreference = RemotePreference.FORCE
        ).mapToPayload()
    }

    fun revokeInviteCode(id: String): Flow<Payload<Unit>> {
        return getDataOnce(
            remote = { remote.revokeInviteCode(id) },
            localUpdate = { local.deleteInviteCode(id) },
            remotePreference = RemotePreference.FORCE
        ).mapToPayload()
    }
}