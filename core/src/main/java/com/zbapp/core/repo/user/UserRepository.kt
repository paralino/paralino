package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.logg
import com.zbapp.core.models.Payload
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.base.asRemoteException
import com.zbapp.core.utils.NotFoundException
import com.zbapp.core.utils.UnauthorizedException
import com.zbapp.core.utils.UnverifiedUserException
import kotlinx.coroutines.flow.*


class UserRepository(
    private val local: IUserLocalDataSource,
    private val remote: UserRemoteSource
) : BaseRepository() {

    companion object {
        var _cachedUser: UserMe? = null
    }

    val cachedUserId: String get() = config.userId
    var cachedUser: UserMe?
        get() = _cachedUser
        set(value) {
            _cachedUser = value
        }

    fun login(email: String, password: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.login(email, password) },
            localUpdate = { config.token = it },
            remotePreference = RemotePreference.FORCE
        ).map { Unit }
    }

    fun signUp(userName: String?, email: String, password: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.signUp(userName, email, password) },
            localUpdate = {
                updateLocalUser(it)
                config.userId = it.id
                config.email = email
                config.token = "loggedIn"
                config.emailConfirmed = false
            },
            remotePreference = RemotePreference.FORCE
        )
            .map { Unit }
    }

    fun signUpAnonymously(): Flow<Unit> {
        return getDataOnce(
            remote = { remote.singUpAnonymously() },
            remotePreference = RemotePreference.FORCE
        ).flatMapLatest {
            getDataOnce(
                remote = { remote.getMe() },
                localUpdate = {
                    updateLocalUser(it)
                    config.userId = it.id
                    config.email = ""
                    config.token = "anon"
                    config.emailConfirmed = true
                },
                remotePreference = RemotePreference.FORCE
            )
        }.map { Unit }
    }

    fun restoreSession(serverUrl: String) {
//        remote.restoreSession(serverUrl, config.token)
    }

    fun getUserLocally(): Flow<UserMe> {
        return local.getMe()
            .onEach { cachedUser = it }
            .catch {
                cachedUser = null
                throw it
            }
    }

    fun getMe(): Flow<Payload<UserMe>> {
        return getUserLocally().map<UserMe, Payload<UserMe>> {
            Payload.Success(it)
        }.onStart { emit(Payload.Loading) }.catch { emit(Payload.Error(it)) }
    }

    fun refreshUserIfAvailable(): Flow<UserMe> {
        if (!config.isTokenValid) {
            return flow { throw Exception("Token is not available") }
        }

        return getDataOnce(
            remote = remote::getMe,
            localUpdate = {
                config.emailConfirmed = true
                updateLocalUser(it)
            },
            remotePreference = RemotePreference.FORCE
        ).catch {
            if (it is NotFoundException) {
                logg.v("User does not exist - create it for the first time")
//                emitAll(initializeUser())
                throw it
            } else {
                if (it is UnverifiedUserException)
                    config.emailConfirmed = false
                logg.e("Error refreshing user")
                throw it
            }
        }
    }
//
//    private fun initializeUser(): Flow<UserMe> {
//        if (!config.isTokenValid) {
//            return flow { throw Exception("Token is not available") }
//        }
//
//        return getDataOnce(
//            remote = {
//                val user = UserMe(
//                    id = config.userId,
//                    email = config.email
//                )
//                remote.initVerifiedUser(user)
//            },
//            localUpdate = {
//                config.emailConfirmed = true
//                updateLocalUser(it)
//            },
//            remotePreference = RemotePreference.FORCE
//        )
//            .catch {
//                if (it is UnverifiedUserException) {
//                    config.emailConfirmed = false
//                }
//                logg.e("Error refreshing user")
//                throw it
//            }
//    }

    private suspend fun updateLocalUser(data: UserMe) {
        config.userId = data.id
        config.email = data.email
        cachedUser = data
        local.update(data)
    }

    fun editUsername(newName: String): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateName(newName) },
            localUpdate = {
                cachedUser?.copy(name = newName)?.let {
                    updateLocalUser(it)
                }
            },
            remotePreference = RemotePreference.FORCE
        ).map { true }
    }

    fun editUserImage(imageBase64: String): Flow<Boolean> {
        return getDataOnce(
            remote = { remote.updateUserImage(imageBase64) },
            localUpdate = {
                cachedUser?.copy(imageBase64 = imageBase64)?.let {
                    updateLocalUser(it)
                }
            },
            remotePreference = RemotePreference.FORCE
        ).map { true }
    }

    suspend fun clear() {
        cachedUser = null
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