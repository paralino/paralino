package app.paralino.core.repo.personal_code

import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.user.UserCryptoKeyState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PersonalCodesRepository constructor(
    private val remote: PersonalCodesRemoteSource,
    private val local: PersonalCodeLocalDataSource,
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val scopeProvider: ICoroutineScopeProvider,
    private val sessionMonitor: UserSessionMonitor,
    private val appPrefs: AppPrefs
) : BaseRepository() {

    val personalCodeLocalState: StateFlow<List<UserPersonalCode>> = userKeyRepo.userKeyState()
        .flatMapLatest { keyState ->
            if (keyState == UserCryptoKeyState.OK) {
                local.getPersonalCodes()
                    .catch {
                        logg.w("Personal codes state error: ${it.message}")
                        emit(emptyList())
                    }
            } else {
                flowOf(emptyList())
            }
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
            val currentCodeCount = personalCodeLocalState.value.size
            val shouldRefresh = try {
                createPersonalCode(null)
                true
            } catch (e: Exception) {
                // this is to handle a case if user is trying to create a code while local state is not synced
                if (e.asRemoteException()?.code == 400) {
                    refreshPersonalCodes()
                    delay(200)
                    if (currentCodeCount <= personalCodeLocalState.value.size) {
                        // there was already a code on the backend, do nothing
                        false
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }

            if (shouldRefresh) {
                refreshPersonalCodes()
            }
        }
    }

    private suspend fun createPersonalCode(userMeData: UserMeData? = null) {
        val user = userRepo.cachedUser!!
//            ?: throw Throwable("User not valid")

        if (!canCreateNewCode()) {
            // can not create new code
            return
        }

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

    private fun getActivePersonalCodeOnce(): UserPersonalCode? {
        return activePersonalCodeState.value
    }

    fun canCreateNewCode(): Boolean {
        return personalCodeLocalState.value.size < userRepo.userPlans.value.highestValidPlanConfig.paralinoIdsLimit
    }

    suspend fun refreshPersonalCodes(cacheDelayMillis: Duration = 0.seconds) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "personal_code_refresh",
                timeMillis = cacheDelayMillis.inWholeMilliseconds
            )
        ) {
            remote.getMyPersonalCodes()
                .also { local.replacePersonalCodes(it) }
        }
    }

    suspend fun refreshPersonalCodesAndRestockKeys(cacheDelayMillis: Duration = 0.seconds) {
        if (sessionMonitor.isSessionValid()) {
            refreshPersonalCodes(cacheDelayMillis = cacheDelayMillis)
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
                    userId = appPrefs.userId,
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
                    userId = appPrefs.userId,
                    userMeData = userMeData
                )
            } else if (canCreateNewCode() && personalCodeLocalState.value.isEmpty()) {
                // even if user can create new code, we should only create one if user has no codes at all
                // this would be the case when account is created
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
                    userId = appPrefs.userId,
                    personalCode = code
                )
            }
        } catch (e: Exception) {
            logg.d("Error while restocking pre keys", e)
        }
    }

    private fun String.cleanCode(): String {
        return this
            .trim()
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .uppercase()
    }
}