package com.zbapp.remote.user

import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserRemoteDataSource
import com.zbapp.remote.ExceptionMapper
import com.zbapp.remote.GroupsRemoteSource
import com.zbapp.remote.api.UserApi

class UserRemoteSource internal constructor(
    private val api: UserApi
): IUserRemoteDataSource {

    constructor(user: RemoteUserCache,
                groupsRemote: GroupsRemoteSource
    ) : this(UserApi(user, groupsRemote))


    override suspend fun getMe(): UserMe {
        try { return api.getMe() }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun updateName(name: String) {
        try { api.updateName(name) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun updateImage(imageBase64: String) {
        try { api.updateImage(imageBase64) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun changePassword(newPassword: String) {
        try { api.changePassword(newPassword) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun generateUsername(): String {
        try { return api.generateUsername() }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun signUp(serverUrl: String, username: String, email: String, password: String): String {
        try { return api.signUp(serverUrl, username, email, password) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun login(serverUrl: String, email: String, password: String): String {
        try { return api.login(serverUrl, email, password) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun logout() {
        try { api.logout() }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun restoreSession(serverUrl: String, accountData: String) {
        try { api.restoreSession(serverUrl, accountData) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun initVerifiedUser(user: UserMe): UserMe {
        try { return api.initVerifiedUser(user) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

}