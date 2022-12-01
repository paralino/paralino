package app.paralino.core.repo.user

import app.paralino.core.models.BackendConfig
import app.paralino.core.models.mappers.UserMapperCore
import app.paralino.core.models.raw.AccountResetCryptoKeyPayload
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMeData
import app.paralino.core.models.user.UserSession
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.IUserApi
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.BaseRemoteException
import app.paralino.core.utils.extensions.blocking
import kotlinx.serialization.encodeToString

class UserRemoteSource constructor(
    private val api: IUserApi,
    private val functionsApi: IFunctionsApi,
    private val remoteConfig: BackendConfig,
    private val userMapper: UserMapperCore,
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

    suspend fun getMe(): UserMeRaw {
        return api.getMe()
    }

    suspend fun getCurrentSession(): UserSession {
        return api.getCurrentSession()
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

    suspend fun initEmailVerification() {
        api.initEmailVerification()
    }

    suspend fun confirmEmail(userId: String, secret: String) {
        api.confirmEmail(userId, secret)
    }

    suspend fun updateUserData(data: UserMeData): UserMeRaw {
        val payload = userMapper.core2Remote(data)
        return api.updateUserData(payload.toPostJson())
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

            val payload = keyToPersist?.let {
                AccountResetCryptoKeyPayload(
                    encryptedSymmetricKey = it.encryptedSymmetricKey,
                    encryptedPrivateKey = it.encryptedPrivateKey,
                    publicKey = it.publicKey
                )
            }

            val data = functionsApi.runAndWait(
                functionId = remoteConfig.resourceIds.functionResetAccount,
                data = payload?.let { BaseRemoteModule.formatter.encodeToString(it) }
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