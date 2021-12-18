package com.zbapp.core.repo.user

import com.zbapp.core.config
import com.zbapp.core.crypto.CryptoAuth
import com.zbapp.core.logg
import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.user.SymmetricCryptoKeyData
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.UnverifiedUserException
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

    fun signUp(userName: String, email: String, password: String): Flow<Unit> {
        return flow {
            // TODO separate executions to signup, encryption setup and upload
            // encrypt user name and sign up
            val user = userRepo.remoteSignUp(userName, email, password).first()

            // generate crypto keys
            val keys = cryptoAuth.generateNewKeysForUser(password, user.first.id, null)

            // if sign up is success, save keys and user locally, backup user key
            cryptoAuth.saveUserKeyLocally(keys.userPersonalKey)
            val data = UserMeData(
                privateKeyBackup = keys.symmetricCryptoKeyData,
                asymmetricKeys = listOf(keys.asymmetricCryptoKeyData)
            )

            val remoteData = userDataMapper.core2Remote(data)

            val remoteUser = userRepo.uploadAndInitKeysAndData(remoteData).first()
            userRepo.initLocallyAfterSignUp(remoteUser, user.second)
            emit(remoteUser)
        }
            .catch {
                logg.e(it)
                throw it
            }
            .map { Unit }
    }

    fun login(email: String, password: String): Flow<Unit> {
        return userRepo.login(email, password)
            .flatMapLatest { session -> userRepo.getUserIfAvailable().map { it to session } }
            .onEach {
                val data: SymmetricCryptoKeyData =
                    BaseRemoteModule.formatter.decodeFromString(it.first.data?.privateKeyBackup!!)
                cryptoAuth.signIn(password, it.first.id, data.key)
                userRepo.initLocallyAfterSignUp(it.first, it.second)
            }
            .flatMapLatest { syncData() }
            .catch {
                logg.e(it)
                if (it is UnverifiedUserException)
                    emit(Unit)
                else throw it
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