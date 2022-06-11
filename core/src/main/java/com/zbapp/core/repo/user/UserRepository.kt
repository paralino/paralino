package com.zbapp.core.repo.user

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.config
import com.zbapp.core.domain.user.UserSessionMonitor
import com.zbapp.core.logg
import com.zbapp.core.models.Resource
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.data
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
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
) : BaseRepository() {

    val cachedUserId: String get() = config.userId

    val localUser: StateFlow<Resource<UserMe?>> = local.getMe()
        .map<UserMe?, Resource<UserMe?>> { Resource.Ready(it) }
        .catch { emit(Resource.NotInitialized) }
        .stateIn(
            scope = scopeProvider.defaultScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = Resource.NotInitialized
        )

    val cachedUser: UserMe? get() = localUser.value.data

    fun login(email: String, password: String): Flow<UserSession> {
        return getDataOnce(
            remote = { remote.login(email, password) },
            localUpdate = { config.token = it.id }
        )
    }

    fun remoteSignUp(
        userName: String?,
        email: String,
        password: String,
    ): Flow<Pair<UserMeRaw, UserSession>> {
        return getDataOnce { remote.signUpAndCreateSession(userName, email, password) }
    }

    suspend fun initLocallyAfterSignUp(userMeRaw: UserMeRaw, userSession: UserSession) {
        updateLocalUser(userMeRaw)
        config.token = userSession.id
        config.emailConfirmed = true
        sessionMonitor.updateSessionState(sessionValid = true)
    }

    fun getUserIfAvailable(): Flow<UserMeRaw> {
        if (!config.isTokenValid) {
            return flow { throw Exception("Token is not available") }
        }

        return getDataOnce { remote.getMe() }
            .catch {
                if (it is UnauthorizedException) {
                    logg.v("User session is not valid")
                } else if (it is NotFoundException) {
                    logg.v("User does not exist")
                } else {
                    if (it is UnverifiedUserException)
                        config.emailConfirmed = false
                    logg.e("Error refreshing user")
                }
                throw it
            }
    }

    fun refreshUserIfAvailable(): Flow<UserMeRaw> {
        return getUserIfAvailable()
            .onEach {
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

    fun uploadAndInitKeysAndData(data: UserMeDataRaw): Flow<UserMeRaw> {
        return getDataOnce { remote.updateUserData(data.toPostJson()) }
    }

    fun updateUserData(data: Map<String, Any>): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateUserData(data) },
            localUpdate = { updateLocalUser(it) }
        ).map { true }
    }

    fun editUsername(newName: String): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateName(newName) },
            localUpdate = { updateLocalUser(it) }
        ).map { true }
    }

    fun editUserImage(image: ImageModel): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateUserImage(image) },
            localUpdate = { updateLocalUser(it) }
        ).map { true }
    }

    suspend fun logoutLocally() {
        local.clear()
        config.clear()
    }

    fun logout(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.logout() },
            localUpdate = {
                logoutLocally()
            }
        ).catch {
            // TODO maybe always log user out? Even if offline or some other error happens, log out locally
            // it will leave active session in the backend
            if (it is UnauthorizedException) {
                logoutLocally()
                emit(Unit)
            } else throw it
        }
    }

    fun deleteAccount(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deleteAccount() },
            localUpdate = {
                logoutLocally()
            }
        ).catch {
            if (it is UnauthorizedException) {
                logoutLocally()
                emit(Unit)
            } else throw it
        }
    }

}