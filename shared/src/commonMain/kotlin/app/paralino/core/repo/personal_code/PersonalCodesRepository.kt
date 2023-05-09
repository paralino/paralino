package app.paralino.core.repo.personal_code

import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.Flow

class PersonalCodesRepository constructor(
    private val remote: PersonalCodesRemoteSource,
    private val local: PersonalCodeLocalDataSource,
    private val userRepo: UserRepository
) : BaseRepository() {

    suspend fun createAndSavePersonalCode() {
        triggerRemoteOnly {
            createPersonalCode()
            remote.getMyPersonalCodes()
        }.also {
            local.replacePersonalCodes(it)
        }
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

    fun getAndRefreshMyPersonalCodes(): Flow<List<UserPersonalCode>> {
        return mergeDataWithRemote(
            local = local.getPersonalCodes(),
            remote = {
                remote.getMyPersonalCodes().also {
                    local.replacePersonalCodes(it)
                }
                tryToRestockPreKeys()
            },
            localUpdate = { }
        )
    }

    suspend fun refreshPersonalCodes() {
        triggerRemoteOnly {
            remote.getMyPersonalCodes()
        }.also {
            local.replacePersonalCodes(it)
        }
    }

    fun getUserInfoFromCode(code: String): Flow<Payload<UserPersonalCodeData>> {
        return getDataOnce(
            remote = { remote.getUserInfoFromCode(code.cleanCode()) }
        ).mapToPayload()
    }

    fun revokePersonalCode(): Flow<Payload<Unit>> {
        return getDataOnce(
            localUpdate = { local.replacePersonalCodes(it) }
        ) {
            remote.revokeAllCodes()
            remote.getMyPersonalCodes()
        }.mapToPayload()
    }

    fun activateUserCode(code: String): Flow<Payload<Unit>> {
        return getDataOnce(
            localUpdate = { local.replacePersonalCodes(it) }
        ) {
            remote.updateCodeUserData(
                personalCode = code.cleanCode(),
                userId = userRepo.cachedUserId,
                userMeData = userRepo.cachedUser!!.data!!
            )
            remote.getMyPersonalCodes()
        }.mapToPayload()
    }

    suspend fun updateUserCode(userMeData: UserMeData) {
        triggerRemoteOnly {
            val currentCode = local.getActivePersonalCodesOnce()
            if (currentCode != null) {
                remote.updateCodeUserData(
                    personalCode = currentCode.code,
                    userId = userRepo.cachedUserId,
                    userMeData = userMeData
                )
            } else {
                // TODO pass data since user details are missing first time code is created
                createPersonalCode()
            }
            remote.getMyPersonalCodes()
        }.also {
            local.replacePersonalCodes(it)
        }
    }

    suspend fun tryToRestockPreKeys() {
        try {
            val code = local.getActivePersonalCodesOnce()?.code?.cleanCode() ?: return
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = "pre_keys_restock",
                    timeMillis = 20_000,
                    failOnError = false
                )
            ) {
                remote.restockPreKeysIfNeeded(
                    userId = userRepo.cachedUserId,
                    personalCode = code
                )
            }
        } catch (e: Exception) {
            logg.d("Error while restocking pre keys", e)
        }
    }

    suspend fun clearLocally() {
        local.clear()
    }

    private fun String.cleanCode(): String {
        return this
            .trim()
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .uppercase()
    }

}