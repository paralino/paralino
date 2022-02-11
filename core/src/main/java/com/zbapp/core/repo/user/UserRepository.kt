package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.logg
import com.zbapp.core.models.Payload
import com.zbapp.core.models.data
import com.zbapp.core.models.mappers.IUserMapperCore
import com.zbapp.core.models.mappers.decrypt
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.utils.NotFoundException
import com.zbapp.core.utils.UnauthorizedException
import com.zbapp.core.utils.UnverifiedUserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*


class UserRepository(
    private val local: IUserLocalDataSource,
    private val remote: UserRemoteSource,
    val mapper: IUserMapperCore
) : BaseRepository() {

    val cachedUserId: String get() = config.userId

    val localUser: StateFlow<Payload<UserMe>> = local.getMe()
        .decrypt(mapper)
        .map<UserMe, Payload<UserMe>> { Payload.Success(it) }
        .catch { emit(Payload.Error(it)) }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.WhileSubscribed(),
            initialValue = Payload.Loading
        )

    val cachedUser: UserMe? get() = localUser.value.data

    fun login(email: String, password: String): Flow<UserSession> {
        return getDataOnce(
            remote = { remote.login(email, password) },
            localUpdate = { config.token = it.id },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun remoteSignUp(
        userName: String?,
        email: String,
        password: String
    ): Flow<Pair<UserMeRaw, UserSession>> {
        return getDataOnce(
            remote = { remote.signUpAndCreateSession(userName, email, password) },
            remotePreference = RemotePreference.FORCE
        )
    }

    suspend fun initLocallyAfterSignUp(userMeRaw: UserMeRaw, userSession: UserSession) {
        updateLocalUser(userMeRaw)
        config.token = userSession.id
        config.emailConfirmed = true
    }

    fun signUpAnonymously(): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.singUpAnonymously().let {
                    remote.getMe() to it
                }
            },
            localUpdate = {
                updateLocalUser(it.first)
                config.token = it.second.id
                config.emailConfirmed = true
            },
            remotePreference = RemotePreference.FORCE
        ).map { Unit }
    }

    fun getUserIfAvailable(): Flow<UserMeRaw> {
        if (!config.isTokenValid) {
            return flow { throw Exception("Token is not available") }
        }

        return getDataOnce(
            remote = remote::getMe,
            remotePreference = RemotePreference.FORCE
        )
            .catch {
                if (it is NotFoundException) {
                    logg.v("User does not exist")
                    throw it
                } else {
                    if (it is UnverifiedUserException)
                        config.emailConfirmed = false
                    logg.e("Error refreshing user")
                    throw it
                }
            }
    }

    fun refreshUserIfAvailable(): Flow<UserMeRaw> {
        return getUserIfAvailable()
            .onEach {
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
        return getDataOnce(
            remote = { remote.updateUserData(data.toPostJson()) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun updateUserData(data: Map<String, Any>): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateUserData(data) },
            localUpdate = { updateLocalUser(it) },
            remotePreference = RemotePreference.FORCE
        ).map { true }
    }

    fun editUsername(newName: String): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateName(newName) },
            localUpdate = { updateLocalUser(it) },
            remotePreference = RemotePreference.FORCE
        ).map { true }
    }

    fun editUserImage(imageBase64: String): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateUserImage(imageBase64) },
            localUpdate = { updateLocalUser(it) },
            remotePreference = RemotePreference.FORCE
        ).map { true }
    }


    private suspend fun clear() {
        local.clear()
    }

    fun logout(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.logout() },
            localUpdate = {
                clear()
                config.clear()
            },
            remotePreference = RemotePreference.FORCE
        ).catch {
            // TODO maybe always log user out? Even if offline or some other error happens, log out locally
            // it will leave active session in the backend
            if (it is UnauthorizedException) {
                clear()
                config.clear()
                emit(Unit)
            } else throw it
        }
    }

    fun deleteAccount(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deleteAccount() },
            localUpdate = {
                clear()
                config.clear()
            },
            remotePreference = RemotePreference.FORCE
        ).catch {
            if (it is UnauthorizedException) {
                clear()
                config.clear()
                emit(Unit)
            } else throw it
        }
    }

}