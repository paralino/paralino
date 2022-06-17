package com.zbapp.core.repo.user

import com.zbapp.core.models.BackendConfig
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.mappers.UserMapperCore
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.repo.api.IFunctionsApi
import com.zbapp.core.repo.api.IUserApi
import com.zbapp.core.utils.exceptions.BaseRemoteException
import com.zbapp.core.utils.extensions.blocking

class UserRemoteSource constructor(
    private val api: IUserApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val userMapper: UserMapperCore
) {

    suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return api.signUp(userName, email, password)
    }

    suspend fun signUpAndCreateSession(
        userName: String?,
        email: String,
        password: String
    ): Pair<UserMeRaw, UserSession> {
        return signUp(userName, email, password).let {
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

    suspend fun logout() {
        api.logout()
    }

}