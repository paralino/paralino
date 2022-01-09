package com.zbapp.remote.appwrite

import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.repo.user.IUserApi
import com.zbapp.core.utils.BaseRemoteModule
import io.appwrite.models.Session
import io.appwrite.models.User
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class UserApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IUserApi {

    override suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return remoteModule.execute {
            account.create(uniqueIdPlaceholder, email, password, userName).getUserFromRemote()
        }
    }

    override suspend fun login(email: String, password: String): UserSession {
        return remoteModule.execute {
            account.createSession(email, password).getSessionFromRemote()
        }
    }

    override suspend fun createAnonymous(): UserSession {
        return remoteModule.execute { account.createAnonymousSession().getSessionFromRemote() }
    }

    override suspend fun getMe(): UserMeRaw {
        return remoteModule.execute { account.get().getUserFromRemote() }
    }

    override suspend fun updateName(name: String): UserMeRaw {
        return remoteModule.execute { account.updateName(name).getUserFromRemote() }
    }

    override suspend fun updateUserData(data: Map<String, Any>): UserMeRaw {
        return remoteModule.execute { account.updatePrefs(data).getUserFromRemote() }
    }

    override suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw {
        return remoteModule.execute {
            account.updatePassword(newPassword, oldPassword).getUserFromRemote()
        }
    }

    override suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return remoteModule.execute { account.updateEmail(email, password).getUserFromRemote() }
    }

    override suspend fun deleteAccount() {
        return remoteModule.execute { account.delete() }
    }

    override suspend fun logout() {
        return remoteModule.execute { account.deleteSessions() }
    }

    private fun User.getUserFromRemote(): UserMeRaw {
        return UserMeRaw(
            id = this.id,
            email = this.email,
            registration = this.registration,
            emailVerification = this.emailVerification,
            data = this.prefs.convertTo {
                BaseRemoteModule.formatter.decodeFromJsonElement<UserMeDataRaw>(
                    BaseRemoteModule.formatter.encodeToJsonElement(it)
                )
            }
        )
    }

    private fun Session.getSessionFromRemote(): UserSession {
        return UserSession(
            id = this.id,
            expire = this.expire,
        )
    }

}