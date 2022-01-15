package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.logg
import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.SymmetricCryptoKeyData
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString

class LoginManager(
    val userRepo: UserRepository,
    val groupRepo: GroupRepository,
    val cryptoAuth: CryptoAuth,
    val userDataMapper: IUserDataMapperCore
) {

    fun isLoggedIn(): Boolean {
        return config.isTokenValid
    }

    fun isVerified(): Boolean {
        return config.emailConfirmed
    }

    fun signUp(userName: String, email: String, password: String): Flow<SignUpStep> {
        return flow {

            emit(SignUpStep.SIGNING_UP)
            val user: Pair<UserMeRaw, UserSession>
            try {
                delay(500)
                // encrypted name can not fit in database field in appwrite
                // user name is saved encrypted to user preferences
                user = userRepo.remoteSignUp(userName = null, email, password).first()
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.SIGNING_UP, e)
            }

            emit(SignUpStep.GENERATING_KEYS)
            val remoteData: UserMeDataRaw
            try {
                delay(500)
                remoteData = prepareUserKeys(password, user.first.id, userName)
            } catch (e: Exception) {
                cryptoAuth.clearUserPersonalKey()
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            emit(SignUpStep.INIT_USER_DATA)
            val updatedUser: UserMeRaw
            try {
                delay(500)
                updatedUser = userRepo.uploadAndInitKeysAndData(remoteData).first()
            } catch (e: Exception) {
                cryptoAuth.clearUserPersonalKey()
                throw SignUpStepException(SignUpStep.INIT_USER_DATA, e)
            }

            emit(SignUpStep.SAVE_USER)
            try {
                delay(500)
                userRepo.initLocallyAfterSignUp(updatedUser, user.second)
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.SAVE_USER, e)
            }

            emit(SignUpStep.ALL_DONE)
        }
            .catch {
                logg.e(it)
                throw it
            }
    }

    private suspend fun prepareUserKeys(password: String, userId: String, name: String): UserMeDataRaw {
        val keys = cryptoAuth.generateNewKeysForUser(password, userId, null)

        cryptoAuth.saveUserKeyLocally(keys.userPersonalKey)
        val data = UserMeData(
            userName = name,
            privateKeyBackup = keys.symmetricCryptoKeyData,
            asymmetricKeys = listOf(keys.asymmetricCryptoKeyData)
        )

        return userDataMapper.core2Remote(data)
    }

    fun login(email: String, password: String): Flow<SignUpStep> {
        return flow {

            emit(SignUpStep.SIGNING_UP)
            val session: UserSession
            val user: UserMeRaw
            try {
                delay(500)
                session = userRepo.login(email, password).first()
                user = userRepo.getUserIfAvailable().first()
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.SIGNING_UP, e)
            }

            emit(SignUpStep.GENERATING_KEYS)
            val data: SymmetricCryptoKeyData
            try {
                delay(500)
                data = BaseRemoteModule.formatter.decodeFromString(user.data?.privateKeyBackup!!)
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            emit(SignUpStep.INIT_USER_DATA)
            try {
                delay(500)
                val userKey = cryptoAuth.signIn(password, user.id, data.encryptedKey)
                cryptoAuth.saveUserKeyLocally(userKey)
            } catch (e: Exception) {
                cryptoAuth.logout()
                throw SignUpStepException(SignUpStep.INIT_USER_DATA, e)
            }

            emit(SignUpStep.SAVE_USER)
            try {
                userRepo.initLocallyAfterSignUp(user, session)
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.SAVE_USER, e)
            }

            emit(SignUpStep.ALL_DONE)
        }.catch {
            logg.e(it)
            throw it
        }
    }

    fun syncUserAndData(): Flow<Unit> {
        return userRepo.refreshUserIfAvailable()
            .flatMapLatest { syncData() }
    }

    fun syncData(): Flow<Unit> {
        return groupRepo.getAllGroups()
            .map { Unit }
    }

}

enum class SignUpStep {
    SIGNING_UP,
    GENERATING_KEYS,
    INIT_USER_DATA,
    SAVE_USER,
    ALL_DONE
}

class SignUpStepException(val step: SignUpStep, cause: Throwable) : Exception(cause)