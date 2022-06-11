package com.zbapp.core.repo.personal_code

import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.models.Payload
import com.zbapp.core.models.raw.PersonalCodeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserPersonalCode
import com.zbapp.core.models.user.UserPersonalCodeData
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.exceptions.ConflictException
import com.zbapp.core.utils.exceptions.asRemoteException
import com.zbapp.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class PersonalCodesRepository(
    private val remote: PersonalCodesRemoteSource,
    private val local: IPersonalCodelLocalDataSource,
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
) : BaseRepository() {

    fun createPersonalCode(): Flow<Payload<UserPersonalCode>> {
        val user = userRepo.cachedUser
            ?: return flow { emit(Payload.Error(Throwable("User not valid"))) }

        return getDataOnce(
            remote = {
                try {
                    tryCreatingCode(user)
                } catch (e: Exception) {
                    if (e.asRemoteException() is ConflictException) {
                        try {
                            // in case of conflict try once more
                            tryCreatingCode(user)
                        } catch (e: Exception) {
                            if (e.asRemoteException() is ConflictException) {
                                // if there is conflict again, throw generic exception
                                throw Exception("Please try again")
                            } else {
                                throw e
                            }
                        }
                    } else {
                        throw e
                    }
                }
            },
            localUpdate = { local.addPersonalCode(it) }
        ).mapToPayload()
    }

    private suspend fun tryCreatingCode(user: UserMe): PersonalCodeRaw {
        val codeData = UserPersonalCodeData(
            ownerId = user.id,
            ownerName = user.data?.userName ?: "",
            ownerImage = user.data?.userImage,
            ownerPublicKey = userKeyRepo.latestKeyPair().publicKey,
            code = createPersonalCodeString()
        )
        return remote.createNewPersonalCode(codeData)
    }

    fun getMyPersonalCodes(): Flow<Payload<List<UserPersonalCode>>> {
        return mergeDataWithRemote(
            remote = { remote.getMyPersonalCodes() },
            local = local.getPersonalCodes(),
            localUpdate = { local.replacePersonalCodes(it) }
        )
            .map { it.filter { it.isStillValid() } }
            .mapToPayload()
    }

    fun getUserInfoFromCode(code: String): Flow<Payload<UserPersonalCodeData>> {
        val cleanedCode = code
            .replace("-", "")
            .replace(" ", "")
            .uppercase()
        return getDataOnce(
            remote = { remote.getUserInfoFromCode(cleanedCode) }
        ).mapToPayload()
    }

    fun revokePersonalCode(id: String): Flow<Payload<Unit>> {
        return getDataOnce(
            localUpdate = { local.deletePersonalCode(id) }
        ) { remote.revokePersonalCode(id) }.mapToPayload()
    }

    suspend fun clearLocally() {
        local.clear()
    }

    private fun createPersonalCodeString(): String {
        // without similar characters: 0,O,1,I
        // code pattern XXXX-XXXX
        val seed = "ABCDEFGHJKLMNPQRSTUVWXYZ2345789"
        val sectionLength = 8
        val sections = 1
        val chars = CharArray(sectionLength * sections + sections - 1) {
            if (it % (sectionLength + 1) == sectionLength) '-'
            else seed[Random.nextInt(seed.length)]
        }
        return chars.concatToString()
    }

}