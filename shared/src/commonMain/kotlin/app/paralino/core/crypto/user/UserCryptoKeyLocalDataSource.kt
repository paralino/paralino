package app.paralino.core.crypto.user

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.provider.IConfig
import kotlinx.coroutines.flow.*

class UserCryptoKeyLocalDataSource constructor(
    private val userKeyMapper: UserKeyMapperCore,
    private val appConfig: IConfig,
    private val database: AppDatabaseHelper
) {

    fun getCachedUserKey(): String {
        return appConfig.userKey
    }

    suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String) {
        appConfig.userKey = decryptedMainKey
        database.userKeysQueries.transaction {
            database.userKeysQueries.clearAll()
            database.userKeysQueries.createUserKey(
                userKeyMapper.remote2Local(key, isActive = true)
            )
        }
    }

    suspend fun getKeyPair(): AsymmetricCryptoKeyData? {
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
                    logg.v("User keys can not be parsed or decrypted", e)
                    false
                }
            }
            .catch { emit(false) }
            .distinctUntilChanged()
    }

    suspend fun clearAllKeys() {
        appConfig.userKey = ""
        database.userKeysQueries.transaction {
            database.userKeysQueries.clearAll()
        }
    }
}