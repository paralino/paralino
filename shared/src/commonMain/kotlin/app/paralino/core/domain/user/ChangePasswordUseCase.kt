package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.MissingUserKeyException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class ChangePasswordUseCase(
    val userRepo: UserRepository,
    val cryptoCore: CryptoCore,
    val userKeyRepository: CryptoUserKeyRepository,
) {

    fun changePassword(
        oldPassword: String,
        newPassword: String,
    ): Flow<Unit> {
        val derivedOldPassword = cryptoCore.derivePassword(oldPassword)
        val derivedNewPassword = cryptoCore.derivePassword(newPassword)

        val userId = userRepo.cachedUserId

        return flow {

            val oldKey = userKeyRepository.getLocalRawKey()
                ?: throw MissingUserKeyException(localCache = false)

            // re-encrypt current key with new password
            val newEncryptedKey = try {
                cryptoCore.reEncryptSymmetricKey(
                    oldBackupPassword = derivedOldPassword.backupPassword,
                    newBackupPassword = derivedNewPassword.backupPassword,
                    userId = userId,
                    encryptedKey = oldKey.encryptedSymmetricKey
                )
            } catch (e: Exception) {
                throw DecryptionException("Can't change encryption key", e)
            }

            // change version
            val newKey = oldKey.copy(
                encryptedSymmetricKey = newEncryptedKey,
                version = nowUTC().toEpochMilliseconds()
            )

            // first upload new key to server as backup, without deleting old keys
            val backedUpKey = userKeyRepository.uploadNewUserKey(newKey, removeOldKeys = false)

            // verify everything is correctly uploaded and matches
            cryptoCore.verifyUserKeysChange(
                oldKey = oldKey,
                oldPassword = derivedOldPassword.backupPassword,
                newKey = backedUpKey,
                newPassword = derivedNewPassword.backupPassword,
                userPersonalKey = userKeyRepository.personalKey()
            )

            // change login password
            userRepo.changePassword(
                oldPassword = derivedOldPassword.loginPassword,
                newPassword = derivedNewPassword.loginPassword
            )

            // if password is changed successfully we can save the new key and delete old ones
            val newRemoteKey = userKeyRepository.uploadNewUserKey(newKey, removeOldKeys = true)

            // at this point both login password and encryption key are updated safely
            // we can save the key locally
            userKeyRepository.saveUserKeyLocally(newRemoteKey, userKeyRepository.personalKey())

            emit(Unit)
        }
    }
}