package app.paralino.core.repo.personal_code

import app.paralino.core.models.Payload
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.raw.PersonalCodeRaw
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.*

class PersonalCodesRepository constructor(
    private val remote: PersonalCodesRemoteSource,
    private val local: IPersonalCodelLocalDataSource,
    private val userRepo: UserRepository
) : BaseRepository() {

    fun createPersonalCode(): Flow<Payload<PersonalCodeRaw>> {
        val user = userRepo.cachedUser
            ?: return flow { emit(Payload.Error(Throwable("User not valid"))) }

        return getDataOnce(
            remote = {
                try {
                    remote.tryCreatingCode(user)
                } catch (e: Exception) {
                    if (e.asRemoteException() is ConflictException) {
                        try {
                            // in case of conflict try once more
                            remote.tryCreatingCode(user)
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

    fun revokeAllCodes(): Flow<Payload<Boolean>> {
        return getDataOnce(
            remote = {
                val allValidCodes = remote.getMyPersonalCodes()
                allValidCodes.forEach {
                    remote.revokePersonalCode(it.id)
                }
                allValidCodes
            },
            localUpdate = { local.clear() }
        )
            .flatMapLatest {
                val flow = if (it.isNotEmpty()) {
                    createPersonalCode().map { it.mapSuccess { true } }
                } else {
                    flowOf(true).mapToPayload()
                }
                flow
            }
    }

    suspend fun clearLocally() {
        local.clear()
    }

}