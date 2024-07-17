package app.paralino.core.domain.user

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKey
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.OldMasterKeys
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
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
            oldPassword = password,
            newPassword = null,
            action = { oldLoginKey, newLoginKey ->
                userRepo.updateUserEmail(
                    newEmail = email,
                    password = oldLoginKey!!
                )
            }
        )
    }

    fun changePassword(oldPassword: String, newPassword: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = userRepo.cachedUserId,
            oldPassword = oldPassword,
            newPassword = newPassword,
            action = { oldLoginKey, newLoginKey ->
                userRepo.changePassword(
                    oldPassword = oldLoginKey!!,
                    newPassword = newLoginKey
                )
            }
        )
    }

    fun changeCredentials(
        userId: String,
        oldPassword: String,
        newPassword: String?,
        action: suspend (String?, String) -> Unit
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

            val newMasterAuthKey = if (newPassword.isNullOrBlank()) {
                oldMasterAuthKey
            } else {
                cryptoCore.deriveUserAuthKey(
                    salt = cryptoCore.randomId(
                        length = 32,
                        alphabet = AlphabetType.EXTENDED
                    ),
                    password = newPassword,
                    alg = cryptoCore.getDefaultLoginKDFAlg()
                )
            }

            emitAll(
                changeCredentials(
                    userId = userId,
                    oldKey = GeneratedUserKeys(
                        userPersonalKey = userKeyRepo.activePersonalKey(),
                        signatureKeyPair = userKeyRepo.signatureKeyPair(),
                        encryptedKeyData = oldKey
                    ),
                    oldMasterAuthKey = oldMasterAuthKey,
                    newMasterAuthKey = newMasterAuthKey,
                    action = action
                )
            )
        }.catch {
            logg.e(it)
            throw it
        }.flowOn(Dispatchers.IO)
    }

    fun changeCredentials(
        userId: String,
        oldKey: GeneratedUserKeys,
        oldMasterAuthKey: UserAuthDerivedKey?,
        newMasterAuthKey: UserAuthDerivedKey,
        action: suspend (String?, String) -> Unit
    ): Flow<ChangeCredentialsStep> {
        return flow {

            emit(ChangeCredentialsStep.KEY_SETUP)

            // re-encrypt current key with new password

            val newUserKeys = cryptoCore.changeUserMasterKey(
                userId = userId,
                signatureKeyPair = userKeyRepo.signatureKeyPair(),
                authKey = newMasterAuthKey
            )

            val oldMasterKeys = oldKey.encryptedKeyData.oldMasterKeys?.let {
                cryptoCore.decryptDataWithKey(
                    data = it,
                    key = oldKey.userPersonalKey
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<OldMasterKeys>(it)
                }
            } ?: OldMasterKeys(keys = emptyList())

            val updatedOldMasterKeys = oldMasterKeys.copy(
                keys = buildList {
                    addAll(oldMasterKeys.keys)
                    add(
                        SymmetricCryptoKeyData(
                            key = oldKey.userPersonalKey,
                            version = oldKey.encryptedKeyData.version
                        )
                    )
                }
            ).let { BaseRemoteModule.formatter.encodeToString(it) }

            // change version
            val newKey = UserCryptoKeyRaw(
                id = oldKey.encryptedKeyData.id,
                version = 0,
                ownerUserId = userId,
                encryptedSymmetricKey = newUserKeys.encryptedKeyData.encryptedSymmetricKey,
                encryptedPrivateKey = newUserKeys.encryptedKeyData.encryptedPrivateKey,
                encryptedPublicKey = newUserKeys.encryptedKeyData.encryptedPublicKey,
                publicKey = newUserKeys.signatureKeyPair.publicKey,
                oldMasterKeys = updatedOldMasterKeys,
                loginSalt = newUserKeys.encryptedKeyData.loginSalt,
                loginAlg = newUserKeys.encryptedKeyData.loginAlg,
                dataAlg = newUserKeys.encryptedKeyData.dataAlg
            )

            // verify everything is correctly done and matches
            if (oldMasterAuthKey != null) {
                try {
                    cryptoCore.verifyUserKeysChange(
                        oldKey = oldKey.encryptedKeyData,
                        newKey = newKey,
                        oldMasterAuthKey = oldMasterAuthKey.masterAuthKey,
                        newMasterAuthKey = newMasterAuthKey.masterAuthKey,
                        oldMasterKey = oldKey.userPersonalKey,
                        newMasterKey = newUserKeys.userPersonalKey
                    )
                } catch (e: Exception) {
                    throw InvalidRequestException(message = e.message, e)
                }
            }

            // first upload new key to server as backup, without deleting old keys
            val backedUpKey = userKeyRepo.uploadNewUserKey(newKey, removeOldKeys = false)

            // check once more everything is properly uploaded
            check(newKey.encryptedSymmetricKey == backedUpKey.encryptedSymmetricKey)
            check(newKey.encryptedPrivateKey == backedUpKey.encryptedPrivateKey)
            check(newKey.encryptedPublicKey == backedUpKey.encryptedPublicKey)
            check(newKey.publicKey == backedUpKey.publicKey)
            check(newKey.oldMasterKeys == backedUpKey.oldMasterKeys)
            check(newKey.loginSalt == backedUpKey.loginSalt)
            check(newKey.loginAlg == backedUpKey.loginAlg)
            check(newKey.dataAlg == backedUpKey.dataAlg)

            emit(ChangeCredentialsStep.CHANGING_CREDENTIALS)

            // action
            action(oldMasterAuthKey?.loginKey, newMasterAuthKey.loginKey)

            // if password is changed successfully we can save the new key and delete old ones
            val newRemoteKey = userKeyRepo.uploadNewUserKey(newKey, removeOldKeys = true)

            // at this point both login password and encryption key are updated safely
            // we can save the key locally
            userKeyRepo.saveUserKeyLocally(keyRaw = newRemoteKey, decryptedKey = newUserKeys.userPersonalKey)

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