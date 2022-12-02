package app.paralino.android.data.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.paralino.core.utils.IConfig

class Config(context: Context) : IConfig {

    companion object {
        const val keyToken = "prefs.profile.token"
        const val keyUserKey = "prefs.user.cryptKey"
        const val keyEmail = "prefs.profile.email"
        const val keyUserId = "prefs.profile.user_id"
        const val keyTempPassword = "prefs.user.temp_password"
        const val keyEmailConfirmed = "prefs.profile.email.confirmed"
        const val keyEmailGuest = "prefs.profile.guest.email"
        const val keyPasswordGuest = "prefs.profile.guest.password"
        const val keyPushTokenUnsynced = "prefs.device.pushToken.unsynced"
        const val keyDeviceName = "prefs.device.name"
        const val keyDeviceID = "prefs.device.ID"
    }

    private val secureSharedData by lazy {
        EncryptedSharedPreferences(context, "shared_prefs_user", MasterKey(context))
    }

    private val persistentSharedData by lazy {
        EncryptedSharedPreferences(context, "shared_prefs_app", MasterKey(context))
    }

    override var deviceId: String
        get() = persistentSharedData.getString(keyDeviceID, "") ?: ""
        set(value) {
            persistentSharedData.edit { putString(keyDeviceID, value) }
        }

    override var sessionId: String
        get() = secureSharedData.getString(keyToken, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyToken, value) }
        }

    override var email: String
        get() = secureSharedData.getString(keyEmail, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyEmail, value) }
        }

    override var deviceName: String
        get() = secureSharedData.getString(keyDeviceName, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyDeviceName, value) }
        }

    override var userId: String
        get() = secureSharedData.getString(keyUserId, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyUserId, value) }
        }

    override var emailConfirmed: Boolean
        get() = secureSharedData.getBoolean(keyEmailConfirmed, false)
        set(value) {
            secureSharedData.edit { putBoolean(keyEmailConfirmed, value) }
        }

    override var temporaryBackupPassword: String
        get() = secureSharedData.getString(keyTempPassword, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyTempPassword, value) }
        }

    override var guestEmail: String
        get() = persistentSharedData.getString(keyEmailGuest, "") ?: ""
        set(value) {
            persistentSharedData.edit { putString(keyEmailGuest, value) }
        }

    override var guestPassword: String
        get() = persistentSharedData.getString(keyPasswordGuest, "") ?: ""
        set(value) {
            persistentSharedData.edit { putString(keyPasswordGuest, value) }
        }

    var userKey: String
        get() = secureSharedData.getString(keyUserKey, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyUserKey, value) }
        }

    override var pushTokenUnsynced: String
        get() = secureSharedData.getString(keyPushTokenUnsynced, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyPushTokenUnsynced, value) }
        }


    override fun clear() {
        secureSharedData.edit { clear() }
    }


    inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String): T? {
        try {
            return enumValueOf<T>(this.getString(key, null) ?: return null)
        } catch (e: Exception) {
            return null
        }
    }

}