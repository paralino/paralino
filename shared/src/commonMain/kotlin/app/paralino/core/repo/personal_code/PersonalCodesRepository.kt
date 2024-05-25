package app.paralino.core.repo.personal_code

import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPersonalCode
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.ConflictException
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.minutes

class PersonalCodesRepository constructor(
    private val remote: PersonalCodesRemoteSource,
    private val local: PersonalCodeLocalDataSource,
    private val userRepo: UserRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val sessionMonitor: UserSessionMonitor
) : BaseRepository() {

    val personalCodeLocalState: StateFlow<List<UserPersonalCode>> = local.getPersonalCodes()
        .catch {
            logg.w("Personal codes state error: ${it.message}")
            emit(emptyList())
        }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val activePersonalCodeState: StateFlow<UserPersonalCode?> = personalCodeLocalState
        .map { it.firstOrNull { it.isStillValid() } }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    suspend fun createAndSavePersonalCode() {
        triggerRemoteOnly {
            createPersonalCode(null)
            refreshPersonalCodes()
        }
    }

    private suspend fun createPersonalCode(userMeData: UserMeData? = null) {
        val user = userRepo.cachedUser!!
//            ?: throw Throwable("User not valid")

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

    fun getActivePersonalCodeOnce(): UserPersonalCode? {
        return activePersonalCodeState.value
    }

    suspend fun refreshPersonalCodes() {
        triggerRemoteOnly {
            remote.getMyPersonalCodes()
                .also { local.replacePersonalCodes(it) }
        }
    }

    suspend fun refreshPersonalCodesAndRestockKeys() {
        if (sessionMonitor.isSessionValid()) {
            refreshPersonalCodes()
            tryToRestockPreKeys(20.minutes.inWholeMilliseconds)
        }
    }

    fun getUserInfoFromCode(code: String): Flow<Payload<UserPersonalCodeData>> {
        return getDataOnce(
            remote = { remote.getUserInfoFromCode(code.cleanCode()) }
        ).mapToPayload()
    }

    fun revokePersonalCode(): Flow<Payload<Unit>> {
        return getDataOnce(
            remote = {
                remote.revokeAllCodes()
                refreshPersonalCodes()
            }
        ).mapToPayload()
    }

    fun activateUserCode(code: String): Flow<Payload<Unit>> {
        return getDataOnce(
            remote = {
                remote.updateCodeUserData(
                    personalCode = code.cleanCode(),
                    userId = userRepo.cachedUserId,
                    userMeData = userRepo.cachedUser!!.data!!
                )
                refreshPersonalCodes()
            }
        ).mapToPayload()
    }

    suspend fun updateUserCode(userMeData: UserMeData) {
        triggerRemoteOnly {
            refreshPersonalCodes()
            val currentCode = getActivePersonalCodeOnce()
            if (currentCode != null) {
                remote.updateCodeUserData(
                    personalCode = currentCode.code,
                    userId = userRepo.cachedUserId,
                    userMeData = userMeData
                )
            } else {
                createPersonalCode(userMeData = userMeData)
            }
            refreshPersonalCodes()
        }
    }

    suspend fun tryToRestockPreKeys(cacheDelayMillis: Long) {
        try {
            val code = getActivePersonalCodeOnce()?.code?.cleanCode() ?: return
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = "pre_keys_restock",
                    timeMillis = cacheDelayMillis,
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