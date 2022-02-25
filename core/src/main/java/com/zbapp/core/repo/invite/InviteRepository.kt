package com.zbapp.core.repo.invite

import com.zbapp.core.config
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.models.Payload
import com.zbapp.core.models.invite.PersonalCodeCreateData
import com.zbapp.core.models.invite.PersonalCodeData
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class InviteRepository(
    private val remote: InviteRemoteSource,
    private val local: IInviteCodeLocalDataSource,
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository
) : BaseRepository() {

    fun createInviteCode(): Flow<Payload<PersonalCodeData>> {
        val user = userRepo.cachedUser
            ?: return flow { emit(Payload.Error(Throwable("User not valid"))) }
        return getDataOnce(
            remote = {
                val invite = PersonalCodeCreateData(
                    ownerId = user.id,
                    ownerName = user.name,
                    ownerImageBase64 = user.data?.imageBase64 ?: "",
                    ownerKey = userKeyRepo.latestKeyPair().publicKey
                )
                remote.createInviteCode(invite)
            },
            localUpdate = { local.addInviteCode(it) }
        ).mapToPayload()
    }

    fun getAllInviteCodes(): Flow<Payload<List<PersonalCodeData>>> {
        return mergeDataWithRemote(
            remote = { remote.getInviteCodes() },
            local = local.getAllInviteCodes(),
            localUpdate = { local.replaceInviteCode(it) }
        ).mapToPayload()
    }

    fun getMyInviteCodes(): Flow<Payload<List<PersonalCodeData>>> {
        return mergeDataWithRemote(
            remote = { remote.getInviteCodes() },
            local = local.getInviteCodesForUser(config.userId),
            localUpdate = { local.replaceInviteCode(it) }
        ).map { it.filter { it.isStillValid() } }.mapToPayload()
    }

    fun getInviteByCode(code: String): Flow<Payload<PersonalCodeReadData>> {
        return getDataOnce(
            remote = { remote.getInviteByCode(code) }
        ).mapToPayload()
    }

    fun revokeInviteCode(id: String): Flow<Payload<Unit>> {
        return getDataOnce(
            localUpdate = { local.deleteInviteCode(id) }
        ) { remote.revokeInviteCode(id) }.mapToPayload()
    }
}