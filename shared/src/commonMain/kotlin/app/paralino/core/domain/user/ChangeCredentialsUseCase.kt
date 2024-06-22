package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.MissingUserKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString

class ChangeCredentialsUseCase(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository,
) {

    fun changeEmail(email: String, password: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = userRepo.cachedUserId,
            newSalt = email,
            oldPassword = password,
            newPassword = password,
            action = { oldLoginKey, newLoginKey ->
                // TODO change to only mail update after salt is changed
                userRepo.updateUserEmail(
                    newEmail = email,
                    password = oldLoginKey
                )
                userRepo.changePassword(
                    oldPassword = oldLoginKey,
                    newPassword = newLoginKey
                )
            }
        )
    }

    fun changePassword(oldPassword: String, newPassword: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = userRepo.cachedUserId,
            newSalt = config.email,
            oldPassword = oldPassword,
            newPassword = newPassword,
            action = { oldLoginKey, newLoginKey ->
                userRepo.changePassword(
                    oldPassword = oldLoginKey,
                    newPassword = newLoginKey
                )
            }
        )
    }

    fun changeCredentials(
        userId: String,
        newSalt: String,
        oldPassword: String,
        newPassword: String,
        action: suspend (String, String) -> Unit
    ): Flow<ChangeCredentialsStep> {
        return flow {
            emit(ChangeCredentialsStep.PREPARING)

            val oldKey = userKeyRepo.getLocalRawKey()
                ?: throw MissingUserKeyException(localCache = false)

            val oldMasterAuthKey = cryptoCore.deriveUserAuthKey(
                salt = oldKey.loginSalt,
                password = oldPassword,
                alg = BaseRemoteModule.formatter.decodeFromString(oldKey.loginAlg)
            )
            val newMasterAuthKey = cryptoCore.deriveUserAuthKey(
                salt = newSalt,
                password = newPassword,
                alg = cryptoCore.getDefaultLoginKDFAlg()
            )

            emit(ChangeCredentialsStep.KEY_SETUP)

            // re-encrypt current key with new password
            val dataKdfAlg = cryptoCore.getDefaultDataKDFAlg()
            val newEncryptedKey = try {
                cryptoCore.encryptUserPersonalKey(
                    userId = userId,
                    masterAuthKey = newMasterAuthKey.masterAuthKey,
                    userPersonalKey = userKeyRepo.personalKey(),
                    alg = dataKdfAlg
                )
            } catch (e: Exception) {
                throw DecryptionException("Can't change encryption key", e)
            }

            // change version
            val newKey = UserCryptoKeyRaw(
                id = oldKey.id,
                version = nowUTC().toEpochMilliseconds(),
                ownerUserId = oldKey.ownerUserId,
                encryptedSymmetricKey = newEncryptedKey,
                encryptedPrivateKey = oldKey.encryptedPrivateKey,
                encryptedPublicKey = oldKey.encryptedPublicKey,
                loginSalt = newSalt,
                loginAlg = BaseRemoteModule.formatter.encodeToString(newMasterAuthKey.kdfAlg),
                dataAlg = BaseRemoteModule.formatter.encodeToString(dataKdfAlg)
            )

            // first upload new key to server as backup, without deleting old keys
            val backedUpKey = userKeyRepo.uploadNewUserKey(newKey, removeOldKeys = false)

            // verify everything is correctly uploaded and matches
            cryptoCore.verifyUserKeysChange(
                oldKey = oldKey,
                newKey = backedUpKey,
                oldMasterAuthKey = oldMasterAuthKey.masterAuthKey,
                newMasterAuthKey = newMasterAuthKey.masterAuthKey,
                userPersonalKey = userKeyRepo.personalKey()
            )

            emit(ChangeCredentialsStep.CHANGING_CREDENTIALS)

            // action
            action(oldMasterAuthKey.loginKey, newMasterAuthKey.loginKey)

            // if password is changed successfully we can save the new key and delete old ones
            val newRemoteKey = userKeyRepo.uploadNewUserKey(newKey, removeOldKeys = true)

            // at this point both login password and encryption key are updated safely
            // we can save the key locally
            userKeyRepo.saveUserKeyLocally(newRemoteKey, userKeyRepo.personalKey())

            emit(ChangeCredentialsStep.DONE)
        }.catch {
            logg.e(it)
            throw it
        }.flowOn(Dispatchers.IO)
    }
}

enum class ChangeCredentialsStep {
    PREPARING,
    KEY_SETUP,
    CHANGING_CREDENTIALS,
    DONE
}