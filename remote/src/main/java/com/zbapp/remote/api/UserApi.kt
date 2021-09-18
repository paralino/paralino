package com.zbapp.remote.api

import com.zbapp.core.repo.user.IUserApi
import com.zbapp.remote.appwrite.AppWriteRemoteModule

class UserApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IUserApi {

    override suspend fun signUp(userName: String, email: String, password: String): String {
        try {
            return remoteModule.account.create(email, password, userName).body!!.string()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

    override suspend fun login(email: String, password: String): String {
        try {
            return remoteModule.account.createSession(email, password).body!!.string()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

    override suspend fun getMe(): String {
        try {
            return remoteModule.account.get().body!!.string()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

    override suspend fun updateName(name: String): String {
        try {
            return remoteModule.account.updateName(name).body!!.string()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

    override suspend fun updateUserData(data: Map<String, String>): String {
        try {
            return remoteModule.account.updatePrefs(data).body!!.string()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

    override suspend fun changePassword(newPassword: String, oldPassword: String?): String {
        try {
            return remoteModule.account.updatePassword(newPassword, oldPassword).body!!.string()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

    override suspend fun logout() {
        try {
            remoteModule.account.deleteSessions()
        } catch (e: Exception) {
            throw remoteModule.mapException(e)
        }
    }

}