package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.user.CryptoAccountOperationV1
import app.paralino.core.crypto.user.CryptoAccountOperationV2
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.logg
import app.paralino.core.models.raw.SchemeVersion
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.OldMasterKeys
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.InternalConnectionException
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.exceptions.MissingUserKeyException
import app.paralino.core.utils.exceptions.UnauthorizedException
import app.paralino.core.utils.exceptions.UnsupportedEncryptionSchemeException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart

class ChangeCredentialsUseCase(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val cryptoAccountOperationV1: CryptoAccountOperationV1,
    private val cryptoAccountOperationV2: CryptoAccountOperationV2,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val userKeysRemoteSource: CryptoUserKeysRemoteSource,
    private val connectionStatus: IConnectionStatusProvider,
    private val appPrefs: AppPrefs,
    private val parentManager: LimitedExecutionParentManager,
) : LimitedExecution(scope = parentManager) {

    override fun isProcessing(key: String): Boolean {
        return isProcessingAny()
    }

    fun changeEmail(email: String, password: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = appPrefs.userId,
            oldEmail = appPrefs.email,
            newEmail = email,
            oldPassword = password,
            newPassword = password,
            replaceKey = false,
            action = { oldLoginKey, newLoginKey ->
                val currentUser = userRepo.refreshUserIfAvailable()
                val lastPasswordUpdate = currentUser.passwordUpdateTime
                val requestTime = nowUTC()

                try {
                    userRepo.updateUserEmailAndPassword(
                        newEmail = email,
                        oldPassword = oldLoginKey,
                        newPassword = newLoginKey
                    )
                } catch (e: Exception) {
                    val updatedUser = userRepo.refreshUserIfAvailable()
                    val isPasswordUpdated = updatedUser.passwordUpdateTime?.let {
                        it > (lastPasswordUpdate ?: requestTime)
                    } ?: false
                    if (updatedUser.email == email && isPasswordUpdated) {
                        // we can assume that data is updated
                    } else {
                        throw e
                    }
                }
            }
        )
            .startProcessing("change_email")
    }

    fun changePassword(oldPassword: String, newPassword: String): Flow<ChangeCredentialsStep> {
        return changeCredentials(
            userId = appPrefs.userId,
            oldEmail = appPrefs.email,
            newEmail = appPrefs.email,
            oldPassword = oldPassword,
            newPassword = newPassword,
            replaceKey = true,
            action = { oldLoginKey, newLoginKey ->
                userRepo.changePassword(
                    oldPassword = oldLoginKey,
                    newPassword = newLoginKey
                )
            }
        )
            .startProcessing("change_password")
    }

    fun changeCredentials(
        userId: String,
        oldEmail: String,
        newEmail: String,
        oldPassword: String,
        newPassword: String,
        replaceKey: Boolean,
        action: suspend (String, String) -> Unit
    ): Flow<ChangeCredentialsStep> {
        return flow {

            val oldKey = userKeyRepo.getActiveLocalRawKey()
                ?: throw MissingUserKeyException(localCache = false)

            val oldKeys = GeneratedUserKeys(
                userPersonalKey = userKeyRepo.activePersonalKeyWithVersion().key,
                signatureKeyPair = userKeyRepo.signatureKeyPair(),
                encryptedKeyData = oldKey
            )

            emitAll(
                changeCredentials(
                    userId = userId,
                    oldEmail = oldEmail,
                    newEmail = newEmail,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    replaceKey = replaceKey,
                    oldKeys = oldKeys,
                    action = action
                )
            )
        }
    }

    fun changeCredentials(
        userId: String,
        oldEmail: String,
        newEmail: String,
        oldPassword: String,
        newPassword: String,
        replaceKey: Boolean,
        oldKeys: GeneratedUserKeys,
        action: (suspend (String, String) -> Unit)?
    ): Flow<ChangeCredentialsStep> {
        return flow {

            val oldKey = oldKeys.encryptedKeyData
            val oldLoginKey = when (oldKey.schemeVersionType) {
                SchemeVersion.V1_0 -> {
                    val oldMasterAuthKey = cryptoAccountOperationV1.deriveUserAuthKey(
                        salt = oldKey.loginSalt,
                        password = oldPassword,
                        alg = BaseRemoteModule.formatter.decodeFromString(oldKey.loginAlg)
                    )

                    cryptoAccountOperationV1.verifyUserKeysChange(
                        oldKey = oldKey,
                        oldMasterAuthKey = oldMasterAuthKey.masterAuthKey,
                        oldMasterKey = oldKeys.userPersonalKey,
                    )
                    oldMasterAuthKey.loginKey
                }
                SchemeVersion.V2_0 -> {
                    try {
                        cryptoAccountOperationV2.verifyUserKeysChange(
                            oldKey = oldKey,
                            oldPassword = oldPassword,
                            oldMasterKey = oldKeys.userPersonalKey,
                        )
                    } catch (e: Exception) {
                        if (e is DecryptionException) {
                            throw UnauthorizedException(code = null, type = null, message = e.message, cause = e)
                        } else {
                            throw e
                        }
                    }
                    val oldMasterAuthKey = cryptoAccountOperationV2.deriveUserAuthKey(
                        email = oldEmail,
                        password = oldPassword,
                        alg = cryptoCore.getDefaultLoginKDFAlg()
                    )
                    oldMasterAuthKey.loginKey
                }
                is SchemeVersion.UNKNOWN -> {
                    throw UnsupportedEncryptionSchemeException(scheme = oldKey.schemeVersionType.key)
                }
            }

            val loginAlg = cryptoCore.getDefaultLoginKDFAlg()

            emitAll(
                changeCredentials(
                    userId = userId,
                    oldKey = oldKeys,
                    loginKDFAlg = loginAlg,
                    newPassword = newPassword,
                    replaceKey = replaceKey,
                    action = if (action != null) {
                        {
                            val newMasterAuthKey = cryptoAccountOperationV2.deriveUserAuthKey(
                                email = newEmail,
                                password = newPassword,
                                alg = loginAlg
                            )
                            action(oldLoginKey, newMasterAuthKey.loginKey)
                        }
                    } else {
                        null
                    }
                )
            )
        }
            .onStart { emit(ChangeCredentialsStep.PREPARING) }
            .catch {
                logg.e(it)
                throw it
            }.flowOn(Dispatchers.IO)
    }

    private fun changeCredentials(
        userId: String,
        oldKey: GeneratedUserKeys,
        loginKDFAlg: KDFAlg,
        newPassword: String,
        replaceKey: Boolean,
        action: (suspend () -> Unit)?
    ): Flow<ChangeCredentialsStep> {
        return flow {

            emit(ChangeCredentialsStep.KEY_SETUP)

            // re-encrypt current key with new password

            val newUserKeys = cryptoAccountOperationV2.changeUserMasterKey(
                userId = userId,
                signatureKeyPair = oldKey.signatureKeyPair,
                password = newPassword,
                loginAlg = loginKDFAlg
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
                dataAlg = newUserKeys.encryptedKeyData.dataAlg,
                dataSalt = newUserKeys.encryptedKeyData.dataSalt,
                schemeVersion = SchemeVersion.V2_0.key
            )

            // verify everything is correctly done and matches
            try {
                cryptoAccountOperationV2.verifyUserKeysChange(
                    oldKey = oldKey.encryptedKeyData,
                    newKey = newKey,
                    newPassword = newPassword,
                    oldMasterKey = oldKey.userPersonalKey,
                    newMasterKey = newUserKeys.userPersonalKey
                )
            } catch (e: Exception) {
                throw InvalidRequestException(message = e.message, e)
            }

            if (replaceKey) {
                // first upload new key to server as backup, without deleting old keys
                val backedUpKey = userKeysRemoteSource.createPersonalKey(newKey, removeOldKeys = false)

                // check once more everything is properly uploaded
                newKey.checkKeysAreValid(backedUpKey = backedUpKey)

                emit(ChangeCredentialsStep.CHANGING_CREDENTIALS)

                // ensure proper network connection before change action
                val networkStatus = connectionStatus.initAndGetStatus(forceCheck = true)
                if (!networkStatus.isOnline) {
                    throw InternalConnectionException(status = networkStatus)
                }

                // action
                action?.invoke()

                emit(ChangeCredentialsStep.KEY_APPLY)

                // if password is changed successfully we can save the new key and delete old ones

                // save key right away
                userKeyRepo.saveUserKey(
                    key = backedUpKey,
                    decryptedMainKey = newUserKeys.userPersonalKey
                )

                // apply to remote
                val newRemoteKey = try {
                    userKeysRemoteSource.createPersonalKey(newKey, removeOldKeys = true)
                } catch (e: Exception) {
                    delay(300)
                    try {
                        // try once again
                        userKeysRemoteSource.createPersonalKey(newKey, removeOldKeys = true)
                    } catch (e: Exception) {
                        delay(300)
                        // get keys to see if already uploaded
                        try {
                            userKeysRemoteSource.getAllMyKeys().firstOrNull { newKey ->
                                try {
                                    newKey.checkKeysAreValid(backedUpKey = backedUpKey)
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            }!!
                        } catch (e: Exception) {
                            // fallback to already uploaded key
                            // on next login or credentials change it will be sorted out
                            backedUpKey
                        }
                    }
                }

                // at this point both login password and encryption key are updated safely
                // we can save the key locally again
                userKeyRepo.saveUserKey(
                    key = newRemoteKey,
                    decryptedMainKey = newUserKeys.userPersonalKey
                )
            } else {
                emit(ChangeCredentialsStep.CHANGING_CREDENTIALS)
                // action
                action?.invoke()
            }

            emit(ChangeCredentialsStep.DONE)
        }.flowOn(Dispatchers.IO)
    }
}

enum class ChangeCredentialsStep {
    PREPARING,
    KEY_SETUP,
    CHANGING_CREDENTIALS,
    KEY_APPLY,
    DONE
}