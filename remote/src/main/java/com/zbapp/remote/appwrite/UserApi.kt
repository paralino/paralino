package com.zbapp.remote.appwrite

import com.zbapp.core.repo.user.IUserApi
import com.zbapp.core.repo.user.SessionResponse
import com.zbapp.core.repo.user.UserResponse

class UserApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IUserApi {

    override suspend fun signUp(userName: String?, email: String, password: String): UserResponse {
        return remoteModule.execute { account.create(email, password, userName).body!!.string() }
    }

    override suspend fun login(email: String, password: String): SessionResponse {
        return remoteModule.execute { account.createSession(email, password).body!!.string() }
    }

    override suspend fun createAnonymous(): SessionResponse {
        return remoteModule.execute { account.createAnonymousSession().body!!.string() }
    }

    override suspend fun getMe(): UserResponse {
        return remoteModule.execute { account.get().body!!.string() }
    }

    override suspend fun updateName(name: String): UserResponse {
        return remoteModule.execute { account.updateName(name).body!!.string() }
    }

    override suspend fun updateUserData(data: Map<String, Any>): UserResponse {
        return remoteModule.execute { account.updatePrefs(data).body!!.string() }
    }

    override suspend fun changePassword(newPassword: String, oldPassword: String?): UserResponse {
        return remoteModule.execute {
            account.updatePassword(newPassword, oldPassword).body!!.string()
        }
    }

    override suspend fun updateEmail(email: String, password: String): UserResponse {
        return remoteModule.execute { account.updateEmail(email, password).body!!.string() }
    }

    override suspend fun deleteAccount() {
        return remoteModule.execute { account.delete() }
    }

    override suspend fun logout() {
        return remoteModule.execute { account.deleteSessions() }
    }

}