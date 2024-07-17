package app.paralino.core.crypto.user

import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.data.db.AppConfig
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.mapToOneOrNull
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserCryptoKeyLocalDataSource constructor(
    private val userKeyMapper: UserKeyMapperCore,
    private val appConfig: AppConfig,
    private val database: AppDatabaseHelper
) {

    suspend fun getCachedUserKey(): String {
        if(appConfig.sessionMasterKey.isBlank() || appConfig.encryptedUserMasterKey.isBlank())
            return ""

        return userKeyMapper.unwrapMasterKey(
            sessionMasterKey = appConfig.sessionMasterKey,
            encryptedUserMasterKey = appConfig.encryptedUserMasterKey
        )
    }

    suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String) {
        val keyData = userKeyMapper.wrapMasterKey(userMasterKey = decryptedMainKey)
        appConfig.sessionMasterKey = keyData.sessionKey
        appConfig.encryptedUserMasterKey = keyData.encryptedKey

        database.userKeysQueries.transaction {
            database.userKeysQueries.clearAll()
            database.userKeysQueries.createUserKey(
                userKeyMapper.remote2Local(key, isActive = true)
            )
        }
    }

    suspend fun getKeyPair(): SimpleKeyPairData? {
        return getMyKey()?.let { userKeyMapper.mapKeyPair(it, getCachedUserKey()) }
    }

    suspend fun getMyKey(): UserCryptoKeyRaw? {
        return listenToMyKey().firstOrNull()
    }

    fun listenToMyKey(): Flow<UserCryptoKeyRaw?> {
        return database.mapToOneOrNull { userKeysQueries.getActive() }
            .map {
                it?.let { userKeyMapper.local2Remote(it, userId = appConfig.userId) }
            }.catch {
                logg.w("Error mapping user raw keys", it)
                emit(null)
            }
    }

    fun userKeyState(): Flow<Boolean> {
        return listenToMyKey()
            .distinctUntilChanged()
            .map {
                try {
                    val userKey = getCachedUserKey()
                    logg.v("User keys changed: master key valid (${userKey.isNotBlank()}), key data valid (${it != null})")
                    userKeyMapper.mapKeyPair(it!!, userKey)
                    logg.v("User keys can be parsed and decrypted")
                    true
                } catch (e: Exception) {
                    logg.v("User keys can not be parsed or decrypted - ${e::class.simpleName}: ${e.message}")
                    false
                }
            }
            .catch { emit(false) }
            .distinctUntilChanged()
    }

    suspend fun clearAllKeys() {
        appConfig.encryptedUserMasterKey = ""
        appConfig.sessionMasterKey = ""
        database.userKeysQueries.transaction {
            database.userKeysQueries.clearAll()
        }
    }
}