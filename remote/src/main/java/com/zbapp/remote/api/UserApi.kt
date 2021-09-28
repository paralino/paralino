package com.zbapp.remote.api

import com.zbapp.core.repo.user.IUserApi
import com.zbapp.core.repo.user.SessionResponse
import com.zbapp.core.repo.user.UserResponse
import com.zbapp.remote.appwrite.AppWriteRemoteModule

class UserApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IUserApi {

    override suspend fun signUp(userName: String, email: String, password: String): UserResponse {
        return remoteModule.execute { account.create(email, password, userName).body!!.string() }
    }

    override suspend fun login(email: String, password: String): SessionResponse {
        return remoteModule.execute { account.createSession(email, password).body!!.string() }
    }

    override suspend fun getMe(): UserResponse {
        return remoteModule.execute { account.get().body!!.string() }
    }

    override suspend fun updateName(name: String): UserResponse {
        return remoteModule.execute { account.updateName(name).body!!.string() }
    }

    override suspend fun updateUserData(data: Map<String, String>): UserResponse {
        return remoteModule.execute { account.updatePrefs(data).body!!.string() }
    }

    override suspend fun changePassword(newPassword: String, oldPassword: String?): UserResponse {
        return remoteModule.execute { account.updatePassword(newPassword, oldPassword).body!!.string() }
    }

    override suspend fun logout() {
        return remoteModule.execute { account.deleteSessions() }
    }

}