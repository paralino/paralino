package app.paralino.core.crypto.user

import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserCryptoKeyLocalDataSource constructor(
    private val userKeyMapper: UserKeyMapperCore,
    private val appPrefs: AppPrefs,
    private val database: MainDatabaseHelper
) {

    suspend fun getCachedUserKey(): SymmetricCryptoKeyData? {
        if(appPrefs.sessionMasterKey.isBlank() || appPrefs.encryptedUserMasterKey.isBlank())
            return null

        return userKeyMapper.unwrapMasterKey(
            sessionMasterKey = appPrefs.sessionMasterKey,
            encryptedUserMasterKey = appPrefs.encryptedUserMasterKey,
            keyVersion = appPrefs.userMasterKeyVersion
        )
    }

    suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String) {
        val keyData = userKeyMapper.wrapMasterKey(userMasterKey = decryptedMainKey)
        appPrefs.sessionMasterKey = keyData.sessionKey
        appPrefs.encryptedUserMasterKey = keyData.encryptedKey
        appPrefs.userMasterKeyVersion = key.userKeyVersion

        database.userKeysDao.recreateUserKeys(userKeyMapper.remote2Local(key, isActive = true))
    }

    suspend fun getKeyPair(): SimpleKeyPairData? {
        return getMyKey()?.let {
            userKeyMapper.mapKeyPair(
                model = it,
                userKey = getCachedUserKey()?.key ?: throw MissingUserKeyException(localCache = true)
            )
        }
    }

    suspend fun getMyKey(): UserCryptoKeyRaw? {
        return listenToMyKey().firstOrNull()
    }

    fun listenToMyKey(): Flow<UserCryptoKeyRaw?> {
        return database.userKeysDao.getActive()
            .map {
                it?.let { userKeyMapper.local2Remote(it, userId = appPrefs.userId) }
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
                    val userKey = getCachedUserKey()?.key
                    logg.v("User keys changed: master key valid (${userKey != null}), key data valid (${it != null})")
                    userKeyMapper.mapKeyPair(it!!, userKey!!)
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
        appPrefs.encryptedUserMasterKey = ""
        appPrefs.sessionMasterKey = ""
        appPrefs.userMasterKeyVersion = 0
        database.userKeysDao.clearAll()
    }
}