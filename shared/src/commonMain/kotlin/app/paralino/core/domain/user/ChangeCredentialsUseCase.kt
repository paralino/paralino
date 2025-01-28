package app.paralino.core.domain.user

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKeyV1
import app.paralino.core.crypto.user.CryptoAccountOperationV1
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
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

class ChangeCredentialsUseCase(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val cryptoAccountOperationV1: CryptoAccountOperationV1,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val userKeysRemoteSource: CryptoUserKeysRemoteSource,
    private val appPrefs: AppPrefs
): LimitedExecution() {

    fun changeEmail(email: String, password: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = appPrefs.userId,
            oldPassword = password,
            newPassword = null,
            action = { oldLoginKey, newLoginKey ->
                userRepo.updateUserEmail(
                    newEmail = email,
                    password = oldLoginKey!!
                )
            }
        )
            .startProcessing("change_email")
    }

    fun changePassword(oldPassword: String, newPassword: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = appPrefs.userId,
            oldPassword = oldPassword,
            newPassword = newPassword,
            action = { oldLoginKey, newLoginKey ->
                userRepo.changePassword(
                    oldPassword = oldLoginKey!!,
                    newPassword = newLoginKey
                )
            }
        )
            .startProcessing("change_password")
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

            val oldMasterAuthKey = cryptoAccountOperationV1.deriveUserAuthKey(
                salt = oldKey.loginSalt,
                password = oldPassword,
                alg = BaseRemoteModule.formatter.decodeFromString(oldKey.loginAlg)
            )

            val newMasterAuthKey = if (newPassword.isNullOrBlank()) {
                oldMasterAuthKey
            } else {
                cryptoAccountOperationV1.deriveUserAuthKey(
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
                        userPersonalKey = userKeyRepo.activePersonalKeyWithVersion().key,
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
        oldMasterAuthKey: UserAuthDerivedKeyV1?,
        newMasterAuthKey: UserAuthDerivedKeyV1,
        action: suspend (String?, String) -> Unit
    ): Flow<ChangeCredentialsStep> {
        return flow {

            emit(ChangeCredentialsStep.KEY_SETUP)

            // re-encrypt current key with new password

            val newUserKeys = cryptoAccountOperationV1.changeUserMasterKey(
                userId = userId,
                signatureKeyPair = oldKey.signatureKeyPair,
                authKey = newMasterAuthKey
            )

            val oldMasterKeys = oldKey.encryptedKeyData.oldMasterKeys
                ?.takeIf { it.isNotBlank() }
                ?.let {
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
                            version = oldKey.encryptedKeyData.userKeyVersion
                        )
                    )
                }
            )
                .let {
                    cryptoCore.encryptModel(
                        data = it,
                        key = newUserKeys.userPersonalKey
                    )
                }

            // change version
            val newKey = UserCryptoKeyRaw(
                id = oldKey.encryptedKeyData.id,
                userKeyVersion = 0,
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
                    cryptoAccountOperationV1.verifyUserKeysChange(
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
            val backedUpKey = userKeysRemoteSource.createPersonalKey(newKey, removeOldKeys = false)

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
            val newRemoteKey = userKeysRemoteSource.createPersonalKey(newKey, removeOldKeys = true)

            // at this point both login password and encryption key are updated safely
            // we can save the key locally
            userKeyRepo.saveUserKey(
                key = newRemoteKey,
                decryptedMainKey = newUserKeys.userPersonalKey
            )

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