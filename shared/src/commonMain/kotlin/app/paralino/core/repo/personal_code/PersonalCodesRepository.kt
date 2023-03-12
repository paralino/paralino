package app.paralino.core.repo.personal_code

import app.paralino.core.models.Payload
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PersonalCodesRepository constructor(
    private val remote: PersonalCodesRemoteSource,
    private val local: PersonalCodeLocalDataSource,
    private val userRepo: UserRepository
) : BaseRepository() {

    suspend fun createAndSavePersonalCode() {
        revokeAllCodes(createNewCode = true)
    }

    private suspend fun createPersonalCode(userMeData: UserMeData?) {
        val user = userRepo.cachedUser
            ?: throw Throwable("User not valid")

        return triggerRemoteOnly {
            try {
                remote.tryCreatingCode(user, userMeData)
            } catch (e: Exception) {
                if (e.asRemoteException() is ConflictException) {
                    try {
                        // in case of conflict try once more
                        remote.tryCreatingCode(user, userMeData)
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

    fun getAndRefreshMyPersonalCodes(): Flow<List<UserPersonalCode>> {
        return mergeDataWithRemote(
            local = local.getPersonalCodes(),
            remote = { remote.getMyPersonalCodes() },
            localUpdate = { local.replacePersonalCodes(it) },
        )
            .map { it.filter { it.isStillValid() } }
    }

    suspend fun refreshPersonalCodes() {
        triggerRemoteOnly {
            remote.getMyPersonalCodes()
        }.also {
            local.replacePersonalCodes(it)
        }
    }

    fun getUserInfoFromCode(code: String): Flow<Payload<UserPersonalCodeData>> {
        val cleanedCode = code
            .trim()
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

    suspend fun revokeAllCodes(createNewCode: Boolean = true, userMeData: UserMeData? = null) {
        triggerRemoteOnly {
            if(createNewCode) {
                createPersonalCode(userMeData)
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