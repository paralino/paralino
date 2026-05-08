package app.paralino.core.domain.user

import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.domain.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.logg
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.utils.exceptions.DecryptionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PasswordConfirmationUseCase(
    private val userKeyLocal: CryptoUserKeysLocalSource,
    private val userKeyRemote: CryptoUserKeysRemoteSource,
    private val recoverAccountUseCase: RecoverAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userCryptoKeyRefreshUseCase: UserCryptoKeyRefreshUseCase,
    private val appPrefs: AppPrefs,
    private val limitedExecutionParentManager: LimitedExecutionParentManager,
) : LimitedExecution(scope = limitedExecutionParentManager) {

    private val executionKey = "password_confirmation"

    suspend fun fetchCurrentUserKeys(): List<UserCryptoKeyRaw> {
        return userKeyRemote.getAllMyKeys()
    }

    fun confirmPasswordAndRefreshKey(password: String): Flow<Unit> {
        return flow {
            logg.tag("password_confirmation").v("Starting password confirmation and key refresh")
            
            // Fetch current keys from remote
            val remoteKeys = fetchCurrentUserKeys()
            
            // If there are multiple keys, logout user to trigger fresh login flow
            if (remoteKeys.size != 1) {
                logg.tag("password_confirmation")
                    .w("Multiple keys found (${remoteKeys.size}), logging out user")
                logoutUseCase.fullLogout(logoutLocallyOnError = true)
                emit(Unit)
                return@flow
            }

            logg.tag("password_confirmation").v("Found single active key, attempting to decrypt")
            
            // Try to decrypt the key with the provided password
            val decryptedKey = try {
                recoverAccountUseCase.getCorrectUserKeyForPassword(
                    keys = remoteKeys,
                    password = password
                )
            } catch (e: Exception) {
                logg.tag("password_confirmation").e("Failed to decrypt key with password", e)
                throw e
            }
            
            if (decryptedKey == null) {
                logg.tag("password_confirmation").w("Password incorrect - could not decrypt key")
                throw DecryptionException(type = "user_key", message = "Invalid password")
            }
            
            logg.tag("password_confirmation").v("Password correct, saving key locally")
            
            // Save the decrypted key locally
            userKeyLocal.saveUserKey(
                key = decryptedKey.encryptedKeyData,
                decryptedMainKey = decryptedKey.userPersonalKey
            )
            
            // Refresh the key status to set it back to OK
            userCryptoKeyRefreshUseCase.updateUserKeyStatusNow()
            
            logg.tag("password_confirmation").v("Password confirmation completed successfully")
            emit(Unit)
        }
            .startProcessing(executionKey)
    }
}
