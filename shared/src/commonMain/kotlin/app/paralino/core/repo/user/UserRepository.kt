package app.paralino.core.repo.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.Resource
import app.paralino.core.models.base.PlanType
import app.paralino.core.models.data
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.BlocklistModel
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserPlan
import app.paralino.core.models.user.UserPlans
import app.paralino.core.models.user.UserSession
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.UnverifiedUserException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn


class UserRepository constructor(
    private val scopeProvider: ICoroutineScopeProvider,
    private val local: UserLocalDataSource,
    private val remote: UserRemoteSource,
    private val sessionMonitor: UserSessionMonitor,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val userMapper: UserMapperCore,
    private val cryptoCore: CryptoCore,
    private val appConfigRepo: AppConfigRepository
) : BaseRepository() {

    val cachedUserId: String get() = config.userId

    val localUser: StateFlow<Resource<UserMe?>> = combine(
        local.getMeRaw(),
        userKeyRepo.userKeyState()
    ) { user, keyState ->
        if (!keyState) {
            Resource.NotInitialized
        } else {
            try {
                Resource.Ready(user?.let { userMapper.remote2Core(it) })
            } catch (e: Exception) {
                Resource.NotInitialized
            }
        }
    }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.NotInitialized
        )

    val localUserRaw: StateFlow<Resource<UserMeRaw?>> = local.getMeRaw()
        .map<UserMeRaw?, Resource<UserMeRaw?>> {
            logg.v("Local raw user changed, valid (${it != null})")
            Resource.Ready(it)
        }
        .catch {
            logg.w("Local raw user error: ${it.message}")
            emit(Resource.NotInitialized)
        }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.NotInitialized
        )

    val userPlans: StateFlow<UserPlans> = combine(
        local.getPlans(),
        appConfigRepo.configState
    ) { plans, config ->
        UserPlans(
            rawPlans = plans,
            config = config
        )
    }
        .catch {
            logg.w("User plans parsing error: ${it.message}")
            emit(UserPlans(emptyList(), appConfigRepo.configState.value))
        }
        .stateIn(
            scope = scopeProvider.defaultIOScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPlans(emptyList(), appConfigRepo.configState.value)
        )

    val cachedUser: UserMe? get() = localUser.value.data

    suspend fun login(email: String, password: String): UserSession {
        return triggerRemoteOnly { remote.login(email, password) }
    }

    suspend fun getAllSessionsFromRemote(): List<UserSession> {
        return triggerRemoteOnly { remote.getAllSessions() }
    }

    suspend fun remoteSignUp(
        email: String,
        password: String,
    ): Pair<UserMeRaw, UserSession> {
        return triggerRemoteOnly { remote.signUpAndCreateSession(email, password) }
    }

    suspend fun initLocallyAfterSignUp(userMeRaw: UserMeRaw, userSession: UserSession) {
        updateLocalUser(userMeRaw)
        config.deviceId = cryptoCore.hashProvider.computeHash(
            "${userMeRaw.id}${config.deviceIdSeed}",
            HashType.SHA256
        )
        config.sessionId = userSession.id
        config.emailConfirmed = true
        sessionMonitor.updateSessionState(sessionValid = true)
    }

    fun updateUserEmail(newEmail: String, password: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updateEmail(newEmail, password) },
            localUpdate = { updateLocalUser(it) }
        ).map { Unit }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        triggerRemoteOnly {
            remote.changePassword(
                newPassword = newPassword,
                oldPassword = oldPassword
            )
        }
    }

    suspend fun initResetPassword(email: String) {
        triggerRemoteOnly {
            remote.initResetPassword(email)
        }
    }

    suspend fun resetPassword(userId: String, secret: String, password: String) {
        triggerRemoteOnly {
            remote.resetPassword(userId, secret, password)
        }
    }

    suspend fun getUserFromRemote(): UserMeRaw {
        return try {
            triggerRemoteOnly { remote.getMe() }
        } catch (e: Exception) {
            if (e is UnauthorizedException) {
                logg.v("User session is not valid")
            } else if (e is NotFoundException) {
                logg.v("User does not exist")
            } else {
                if (e is UnverifiedUserException)
                    config.emailConfirmed = false
                logg.e("Error refreshing user")
            }
            throw e
        }
    }

    suspend fun refreshUserIfAvailable(): UserMeRaw {
        return getUserFromRemote()
            .also { updateLocalUser(it) }
            .also {
                try {
                    remote.getPlans().also { local.savePlans(it) }
                } catch (e: Exception) {
                    logg.w("Error refreshing user plans: ${e.message}")
                }
            }
    }

    suspend fun refreshUserMaybe(): Payload<UserMeRaw?> {
        try {
            return triggerRemoteMaybe(
                remote = { refreshUserIfAvailable() },
                remotePreference = RemotePreference.CACHE("user_call", 10_000)
            ).let { Payload.Success(it) }
        } catch (e: Exception) {
            return Payload.Error(e)
        }
    }

    suspend fun updateLocalUser(data: UserMeRaw) {
        config.userId = data.id
        config.email = data.email
        local.update(data)
    }

    private suspend fun updateUserDataBlocking(data: UserMeData) {
        triggerRemoteOnly { remote.updateUserData(data) }
            .also { updateLocalUser(it) }
    }

    suspend fun initEmailVerification() {
        triggerRemoteOnly { remote.initEmailVerification() }
    }

    suspend fun confirmEmail(userId: String, secret: String) {
        triggerRemoteOnly { remote.confirmEmail(userId, secret) }
    }

    fun getBlocklistLocally(): Flow<List<BlocklistModel>> {
        return local.getBlocklist()
    }

    fun getBlocklist(): Flow<List<BlocklistModel>> {
        return mergeDataWithRemote(
            local = local.getBlocklist(),
            remote = { remote.getBlocklist() },
            localUpdate = { local.recreateBlocklist(it) }
        )
    }

    suspend fun refreshBlocklist() {
        triggerRemoteOnly { remote.getBlocklist() }
            .also { local.recreateBlocklist(it) }
    }

    suspend fun removeUserFromBlocklist(userId: String) {
        triggerRemoteOnly {
            val id = local.getBlocklist().first().first { it.resourceId == userId }.id
            remote.unblock(id)
            id
        }
            .also { local.removeFromBlocklist(it) }
    }

    suspend fun removeFromBlocklist(id: String) {
        triggerRemoteOnly { remote.unblock(id) }
            .also { local.removeFromBlocklist(id) }
    }

    suspend fun logoutLocally() {
        local.clear()
    }

    fun remoteLogout(): Flow<Unit> {
        return getDataOnce { remote.logout() }
    }

    fun deleteAccount(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deleteAccount() }
        ).onEach {
            config.guestEmail = ""
            config.guestPassword = ""
        }
    }

    fun resetAccount(keyToPersist: UserCryptoKeyRaw?): Flow<Unit> {
        return getDataOnce(
            remote = { remote.resetAccount(keyToPersist) }
        )
    }

}