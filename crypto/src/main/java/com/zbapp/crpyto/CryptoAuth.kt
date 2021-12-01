package com.zbapp.crpyto


import android.content.Context
import com.virgilsecurity.android.common.exception.*
import com.virgilsecurity.android.ethreeenclave.interaction.EThree
import com.virgilsecurity.android.ethreeenclave.interaction.model.EThreeParams
import com.zbapp.core.repo.base.IUserCrypto


class CryptoAuth(
    private val context: Context,
    identity: String
): IUserCrypto {

    internal val ethree = EThree(
        EThreeParams(
            identity = identity,
            tokenCallback = { "" },
            context = context
        )
    )

    suspend fun signInOrSignUp(backupPassword: String) {
        if(!ethree.hasLocalPrivateKey())
            signIn(backupPassword)
        else signUp(backupPassword)
    }

    suspend fun logout() {
        blocking {
            if(ethree.hasLocalPrivateKey())
                ethree.cleanup()
        }
    }

    suspend fun changePassword(oldBackupPassword: String, newBackupPassword: String) {
        blocking { ethree.changePassword(oldBackupPassword, newBackupPassword).execute() }
    }

    suspend fun resetPassword(newBackupPassword: String) {
        blocking {
            // clear everything
            logout()
            ethree.resetPrivateKeyBackup().execute()

            // generate new key
            ethree.rotatePrivateKey().execute()

            // backup new key
            backupKey(newBackupPassword)
        }
    }

    suspend fun deleteAccount() {
        blocking {
            try {
                ethree.resetPrivateKeyBackup().execute()
            } catch (e: Exception) { }

            try {
                ethree.unregister().execute()
            } catch (e: Exception) {
                when {
                    e.isEThreeException(EThreeException.Description.MISSING_PRIVATE_KEY.errorCode) -> {}
                    e.isEThreeException(EThreeException.Description.USER_IS_NOT_REGISTERED.errorCode) -> {}
                    else -> throw e
                }
            }
        }
    }

    private suspend fun signIn(backupPassword: String) {
        blocking {
            ethree.restorePrivateKey(backupPassword).execute()
        }
    }

    private suspend fun signUp(backupPassword: String) {
        blocking {
            ethree.register().execute()
            ethree.backupPrivateKey(backupPassword).execute()
        }
    }

    private suspend fun backupKey(password: String) {
        blocking {
            ethree.backupPrivateKey(password).execute()
        }
    }

    override suspend fun encryptData(data: String): String {
        return ethree.authEncrypt(data)
    }

    override suspend fun decryptData(data: String): String {
        return ethree.authDecrypt(data)
    }

    companion object {
        fun getDerivedPassword(password: String): DerivedPasswords {
            return EThree.derivePasswords(password).let {
                DerivedPasswords(it.loginPassword, it.backupPassword)
            }
        }
    }
}

data class DerivedPasswords(
    val loginPassword: String,
    val backupPassword: String
)

fun Exception.isEThreeException(code: Int): Boolean {
    return when(this) {
        is EThreeException -> this.description.errorCode == code
        is EThreeRatchetException -> this.description.errorCode == code
        is FindUsersException -> this.description.errorCode == code
        is FileGroupStorageException -> this.description.errorCode == code
        is GroupException -> this.description.errorCode == code
        is RawGroupException -> this.description.errorCode == code
        is SQLiteStorageException -> this.description.errorCode == code
        is TemporaryChannelException -> this.description.errorCode == code
        else -> false
    }
}