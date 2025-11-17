package app.paralino.core.crypto.user

import app.paralino.core.crypto.PersonalSpaceDeviceKey
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.user.UserDeviceEntity
import app.paralino.core.domain.user.UserCryptoKeyState
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CryptoUserKeysLocalSource(
    private val userKeyMapper: UserKeyMapperCore,
    private val database: MainDatabaseHelper,
    private val appPrefs: AppPrefs,
) : BaseRepository() {

    suspend fun activePersonalKeyWithVersion(): SymmetricCryptoKeyData {
        return getCachedUserKey()
            .takeIf { it != null && it.key.isNotBlank() }
            ?: throw MissingUserKeyException(localCache = true)
    }

    suspend fun personalKey(version: Long? = null): SymmetricCryptoKeyData {
        val rawKey = getActiveLocalRawKey() ?: throw MissingUserKeyException(localCache = false)

        if (version == rawKey.userKeyVersion || version == 0L || version == null) {
            return activePersonalKeyWithVersion()
        }

        val oldKeys = rawKey.oldMasterKeys?.let {
            userKeyMapper.remote2Core(oldMasterKeys = it, userKey = activePersonalKeyWithVersion().key)
        } ?: throw MissingUserKeyException(localCache = false)

        val key = oldKeys.keys.firstOrNull {
            it.version == version
        } ?: throw MissingUserKeyException(localCache = false)

        return key
    }

    suspend fun signatureKeyPair(): SimpleKeyPairData {
        return getKeyPair() ?: throw MissingUserKeyException(localCache = false)
    }

    private suspend fun getCachedUserKey(): SymmetricCryptoKeyData? {
        if(appPrefs.sessionMasterKey.isBlank() || appPrefs.encryptedUserMasterKey.isBlank())
            return null

        return userKeyMapper.unwrapMasterKey(
            sessionMasterKey = appPrefs.sessionMasterKey,
            encryptedUserMasterKey = appPrefs.encryptedUserMasterKey,
            keyVersion = appPrefs.userMasterKeyVersion
        )
    }

    suspend fun saveUserKey(key: UserCryptoKeyRaw, decryptedMainKey: String) {
        val keyData = userKeyMapper.wrapMasterKey(userMasterKey = decryptedMainKey)
        appPrefs.sessionMasterKey = keyData.sessionKey
        appPrefs.encryptedUserMasterKey = keyData.encryptedKey
        appPrefs.userMasterKeyVersion = key.userKeyVersion

        database.userKeysDao.recreateUserKeys(userKeyMapper.remote2Local(key, isActive = true))
    }

    suspend fun addNewInactiveUserKey(key: UserCryptoKeyRaw) {
        database.userKeysDao.createUserKey(userKeyMapper.remote2Local(key, isActive = false))
    }

    private suspend fun getKeyPair(): SimpleKeyPairData? {
        return getActiveLocalRawKey()?.let {
            userKeyMapper.mapKeyPair(
                model = it,
                userKey = getCachedUserKey()?.key ?: throw MissingUserKeyException(localCache = true)
            )
        }
    }

    suspend fun getActiveLocalRawKey(): UserCryptoKeyRaw? {
        try {
            val activeKey = database.userKeysDao.getActiveOnce() ?: return null
            return userKeyMapper.local2Remote(model = activeKey, userId = appPrefs.userId)
        } catch (e: Exception) {
            logg.w("Error mapping user raw keys", e)
            return null
        }
    }

    fun userKeyState(): Flow<UserCryptoKeyState> {
        return database.userKeysDao.getAll()
            .distinctUntilChanged()
            .map { keys ->
                val activeKey = keys.firstOrNull { it.isActive }
                val activeCount = keys.count { it.isActive }

                if (activeKey == null || activeCount != 1) {
                    UserCryptoKeyState.INVALID
                } else {
                    val model = try {
                        userKeyMapper.local2Remote(model = activeKey, userId = appPrefs.userId)
                    } catch (e: Exception) {
                        null
                    }

                    val result = try {
                        val userKey = getCachedUserKey()?.key
                        logg.v("User keys changed: master key valid (${userKey != null}), key data valid (${model != null})")
                        userKeyMapper.mapKeyPair(model!!, userKey!!)
                        logg.v("User keys can be parsed and decrypted")
                        UserCryptoKeyState.OK
                    } catch (e: Exception) {
                        logg.v("User keys can not be parsed or decrypted - ${e::class.simpleName}: ${e.message}")
                        UserCryptoKeyState.INVALID
                    }

                    if (result == UserCryptoKeyState.OK && keys.size > 1) {
                        UserCryptoKeyState.EXPIRED
                    } else {
                        result
                    }
                }
            }
            .catch { emit(UserCryptoKeyState.INVALID) }
            .distinctUntilChanged()
    }

    suspend fun getDeviceSenderKeyForLocationId(locationId: String): PersonalSpaceDeviceKey {
        val device = database.userDeviceDao.getUserDeviceOnce(id = locationId)
        if (device == null) {
            return PersonalSpaceDeviceKey.Error.Missing
        }

        return getDeviceSenderKeyForDevice(device = device)
    }

    suspend fun getDeviceSenderKeyForDevice(device: UserDeviceEntity): PersonalSpaceDeviceKey {

        if (!device.hasValidLocation || device.locationId.isNullOrBlank() || device.userKeyVersion == null || device.encryptedSenderKey.isNullOrBlank()) {
            return PersonalSpaceDeviceKey.Error.Missing
        }

        val personalKey = try {
            personalKey(version = device.userKeyVersion)
        } catch (e: Exception) {
            if (e is MissingUserKeyException) {
                return PersonalSpaceDeviceKey.Error.MissingMainKey
            } else {
                return PersonalSpaceDeviceKey.Error.InvalidMainKey
            }
        }

        return try {
            PersonalSpaceDeviceKey.Valid(
                key = userKeyMapper.unwrapDeviceSenderKey(
                    encryptedSenderKey = device.encryptedSenderKey,
                    userKey = personalKey.key
                )
            )
        } catch (e: Exception) {
            PersonalSpaceDeviceKey.Error.Invalid
        }
    }

    suspend fun clearAllKeys() {
        appPrefs.encryptedUserMasterKey = ""
        appPrefs.sessionMasterKey = ""
        appPrefs.userMasterKeyVersion = 0
    }
}