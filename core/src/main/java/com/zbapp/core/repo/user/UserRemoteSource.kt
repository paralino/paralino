package com.zbapp.core.repo.user

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.mappers.UserMapperCore
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.repo.api.IUserApi
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.exceptions.BaseRemoteException
import com.zbapp.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class UserRemoteSource constructor(
    private val api: IUserApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val userMapper: UserMapperCore
) {

    suspend fun signUp(email: String, password: String): UserMeRaw {
        return api.signUp(userName = null, email, password)
    }

    suspend fun signUpAndCreateSession(
        email: String,
        password: String
    ): Pair<UserMeRaw, UserSession> {
        return signUp(email, password).let {
            it to login(email, password)
        }
    }

    suspend fun login(email: String, password: String): UserSession {
        return api.login(email, password)
    }

    suspend fun singUpAnonymously(): UserSession {
        return api.createAnonymous()
    }

    suspend fun getMe(): UserMeRaw {
        return api.getMe()
    }

    suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw {
        return api.changePassword(newPassword, oldPassword)
    }

    suspend fun initResetPassword(email: String) {
        api.initResetPassword(email)
    }

    suspend fun resetPassword(userId: String, secret: String, password: String) {
        api.resetPassword(userId, secret, password)
    }

    suspend fun updateUserData(data: UserMeData): UserMeRaw {
        val payload = userMapper.core2Remote(data)
        return api.updateUserData(payload.toPostJson())
    }

    suspend fun updateUserData(data: Map<String, Any>): UserMeRaw {
        return api.updateUserData(data)
    }

    suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return api.updateEmail(email, password)
    }

    suspend fun deleteAccount() {
        blocking {
            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionDeleteAccountAndData
            )

            if (!data.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to delete user",
                    Exception(data.stderr)
                )
            }
        }
    }

    suspend fun resetAccount(keyToPersist: UserCryptoKeyRaw?) {
        blocking {
            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionResetAccount,
                data = keyToPersist?.let { BaseRemoteModule.formatter.encodeToString(it) }
            )

            if (!data.isSuccess) {
                // TODO throw better exception
                throw BaseRemoteException.fromCode(
                    null,
                    "Unable to reset user",
                    Exception(data.stderr)
                )
            }
        }
    }

    suspend fun logout() {
        api.logout()
    }

}