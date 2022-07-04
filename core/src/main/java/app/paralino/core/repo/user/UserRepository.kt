package app.paralino.core.repo.user

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.config
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.domain.user.UserSessionMonitor
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserSession
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.UnverifiedUserException
import kotlinx.coroutines.flow.*


class UserRepository constructor(
    private val scopeProvider: ICoroutineScopeProvider,
    private val local: IUserLocalDataSource,
    private val remote: UserRemoteSource,
    private val sessionMonitor: UserSessionMonitor,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val userMapper: UserMapperCore,
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
            scope = scopeProvider.defaultScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = Resource.NotInitialized
        )

    val localUserRaw: StateFlow<Resource<UserMeRaw?>> = local.getMeRaw()
        .map<UserMeRaw?, Resource<UserMeRaw?>> { Resource.Ready(it) }
        .catch { emit(Resource.NotInitialized) }
        .stateIn(
            scope = scopeProvider.defaultScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = Resource.NotInitialized
        )

    val cachedUser: UserMe? get() = localUser.value.data

    suspend fun login(email: String, password: String): UserSession {
        return triggerRemoteOnly { remote.login(email, password) }
            .also { config.token = it.id }
    }

    suspend fun remoteSignUp(
        email: String,
        password: String,
    ): Pair<UserMeRaw, UserSession> {
        return triggerRemoteOnly { remote.signUpAndCreateSession(email, password) }
    }

    suspend fun initLocallyAfterSignUp(userMeRaw: UserMeRaw, userSession: UserSession) {
        updateLocalUser(userMeRaw)
        config.token = userSession.id
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
        if (!config.isTokenValid) {
            throw Exception("Token is not available")
        }

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
            .also {
                sessionMonitor.updateSessionState(sessionValid = true)
                config.emailConfirmed = true
                updateLocalUser(it)
            }
    }

    private suspend fun updateLocalUser(data: UserMeRaw) {
        config.userId = data.id
        config.email = data.email
        local.update(data)
    }

    suspend fun uploadAndInitKeysAndData(data: UserMeDataRaw): UserMeRaw {
        return triggerRemoteOnly { remote.updateUserData(data.toPostJson()) }
    }

    fun updateUserData(data: UserMeData): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateUserData(data) },
            localUpdate = { updateLocalUser(it) }
        ).map { true }
    }

    suspend fun logoutLocally() {
        local.clear()
        config.clear()
    }

    fun remoteLogout(): Flow<Unit> {
        return getDataOnce { remote.logout() }
    }

    fun deleteAccount(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deleteAccount() }
        )
    }

    fun resetAccount(keyToPersist: UserCryptoKeyRaw?): Flow<Unit> {
        return getDataOnce(
            remote = { remote.resetAccount(keyToPersist) }
        )
    }

}