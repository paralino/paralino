package app.paralino.core.domain.user

import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.domain.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.LimitedExecutionParentManager
import app.paralino.core.models.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserCryptoKeyRefreshUseCase(
    private val userKeyLocal: CryptoUserKeysLocalSource,
    private val userKeyRemote: CryptoUserKeysRemoteSource,
    private val appPrefs: AppPrefs,
    private val limitedExecutionParentManager: LimitedExecutionParentManager,
) : LimitedExecution(scope = limitedExecutionParentManager) {

    private val executionKey = "user_key_refresh"

    private val _keyStatus = MutableStateFlow<Resource<UserCryptoKeyState>>(Resource.NotInitialized)
    val keyStatus = _keyStatus.asStateFlow()

    suspend fun checkUserKeyStatus() {
        try {
            startProcessing(
                key = executionKey
            )
        } catch (e: Exception) {
            return
        }

        try {
            updateUserKeyStatusNow()
        } finally {
            stopProcessing(key = executionKey)
        }
    }

    suspend fun updateUserKeyStatusNow() {
        val allKeys = try {
            userKeyRemote.getAllMyKeys()
        } catch (e: Exception) {
            return
        }

        if (allKeys.size != 1) {
            // we only need to check for currently active keys
            // if there are more keys, that means that credentials change either failed or not fully finished
            // that is not handled here, case with multiple keys is handled on re-login
            return
        }

        val activeRemoteKey = allKeys.first()

        val needsPasswordCheck = try {
            val currentKey = userKeyLocal.getActiveLocalRawKey()!!
            activeRemoteKey.checkKeysAreValid(currentKey)
            check(activeRemoteKey.userKeyVersion == currentKey.userKeyVersion)
            false
        } catch (e: Exception) {
            true
        }

        if (needsPasswordCheck) {
            userKeyLocal.addNewInactiveUserKey(key = activeRemoteKey)
            _keyStatus.update { Resource.Ready(UserCryptoKeyState.EXPIRED) }
        } else {
            _keyStatus.update { Resource.Ready(UserCryptoKeyState.OK) }
        }
    }
}

enum class UserCryptoKeyState {
    OK, EXPIRED, INVALID;

    val isExpired: Boolean get() = this == EXPIRED
}