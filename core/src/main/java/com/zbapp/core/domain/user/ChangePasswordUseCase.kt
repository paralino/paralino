package com.zbapp.core.domain.user

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.exceptions.DecryptionException
import com.zbapp.core.utils.exceptions.MissingUserKeyException
import com.zbapp.core.utils.extensions.nowUTC
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
                version = nowUTC().epochSeconds
            )

            // first upload new key to server as backup
            val newRemoteKey = userKeyRepository.uploadNewUserKey(newKey)

            // change login password
            userRepo.changePassword(
                oldPassword = derivedOldPassword.loginPassword,
                newPassword = derivedNewPassword.loginPassword
            )

            // if password is saved successfully we can save the key locally
            userKeyRepository.saveUserKeyLocally(newRemoteKey, userKeyRepository.personalKey)

            // at this point both login password and encryption key are updated safely
            // we can delete old key

            try {
                userKeyRepository.deleteKey(oldKey.id).first()
            } catch (e: Exception) {
                // TODO review if it is good approach to leave old alive
                // ignore if deleting fail, will be deleted on next login
            }

            emit(Unit)
        }
    }

}