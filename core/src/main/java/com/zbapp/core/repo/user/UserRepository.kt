package com.zbapp.core.repo.user

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.config
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.domain.user.UserSessionMonitor
import com.zbapp.core.logg
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.models.mappers.UserMapperCore
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.utils.exceptions.NotFoundException
import com.zbapp.core.utils.exceptions.UnauthorizedException
import com.zbapp.core.utils.exceptions.UnverifiedUserException
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