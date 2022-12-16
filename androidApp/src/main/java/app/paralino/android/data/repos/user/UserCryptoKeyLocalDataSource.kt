package app.paralino.android.data.repos.user

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.paralino.android.data.config.Config
import app.paralino.core.crypto.user.ICryptoUserKeyLocalDataSource
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import kotlinx.coroutines.flow.*

class UserCryptoKeyLocalDataSource constructor(
    private val userKeyMapper: UserKeyMapperCore,
    private val appConfig: Config,
) : ICryptoUserKeyLocalDataSource {

    override fun getCachedUserKey(): String {
        return appConfig.userKey
    }

    override suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String) {
        appConfig.userKey = decryptedMainKey
        appConfig.dataStoreUserKeys.edit {
            it.clear()
            it[stringPreferencesKey("publicKey")] = key.publicKey
            it[stringPreferencesKey("encryptedPrivateKey")] = key.encryptedPrivateKey
            it[stringPreferencesKey("encryptedSymmetricKey")] = key.encryptedSymmetricKey
            it[longPreferencesKey("version")] = key.version
        }
    }

    override suspend fun getKeyPair(): AsymmetricCryptoKeyData? {
        return getMyKey()?.let { userKeyMapper.mapKeyPair(it, getCachedUserKey()) }
    }

    override suspend fun getMyKey(): UserCryptoKeyRaw? {
        return listenToMyKey().firstOrNull()
    }

    private fun listenToMyKey(): Flow<UserCryptoKeyRaw?> {
        return appConfig.dataStoreUserKeys.data.map { prefs ->
            if (prefs.asMap().isEmpty())
                return@map null

            UserCryptoKeyRaw(
                id = "",
                ownerUserId = appConfig.userId,
                publicKey = prefs[stringPreferencesKey("publicKey")]!!,
                encryptedPrivateKey = prefs[stringPreferencesKey("encryptedPrivateKey")]!!,
                encryptedSymmetricKey = prefs[stringPreferencesKey("encryptedSymmetricKey")]!!,
                version = prefs[longPreferencesKey("version")]!!,
            )
        }.catch { emit(null) }
    }

    override fun userKeyState(): Flow<Boolean> {
        return listenToMyKey()
            .map {
                try {
                    userKeyMapper.mapKeyPair(it!!, getCachedUserKey())
                    logg.v("User keys can be parsed and decrypted")
                    true
                } catch (e: Exception) {
                    logg.v("User keys can not be parsed or decrypted")
                    false
                }
            }
            .catch { emit(false) }
            .distinctUntilChanged()
    }

    override suspend fun clearAllKeys() {
        appConfig.userKey = ""
        appConfig.dataStoreUserKeys.edit {
            it.clear()
        }
    }
}