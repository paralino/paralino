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

    suspend fun createAndSavePersonalCode() {
        revokeAllCodes(createNewCode = true)
    }

    private suspend fun createPersonalCode() {
        val user = userRepo.cachedUser
            ?: throw Throwable("User not valid")

        return triggerRemoteOnly {
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
        }
    }

    fun getMyPersonalCodes(): Flow<List<UserPersonalCode>> {
        return mergeDataWithRemote(
            local = local.getPersonalCodes(),
            remote = { remote.getMyPersonalCodes() },
            localUpdate = { local.replacePersonalCodes(it) },
        )
            .map { it.filter { it.isStillValid() } }
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

    suspend fun revokeAllCodes(createNewCode: Boolean = true) {
        triggerRemoteOnly {
            if(createNewCode) {
                createPersonalCode()
                remote.getMyPersonalCodes()
            } else {
                remote.revokeAllCodes()
                emptyList()
            }
        }.also {
            local.replacePersonalCodes(it)
        }
    }

    suspend fun clearLocally() {
        local.clear()
    }

}