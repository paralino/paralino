package app.paralino.android.data.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.paralino.core.provider.IConfig


class Config(context: Context) : IConfig() {

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

    override var encryptedUserMasterKey: String
        get() = secureSharedData.getString(keyUserKey, "") ?: ""
        set(value) {
            secureSharedData.edit(commit = true) { putString(keyUserKey, value) }
        }

    override var sessionMasterKey: String
        get() = secureSharedData.getString(keySessionKey, "") ?: ""
        set(value) {
            secureSharedData.edit(commit = true) { putString(keySessionKey, value) }
        }

    override var pushTokenUnsynced: String
        get() = secureSharedData.getString(keyPushTokenUnsynced, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyPushTokenUnsynced, value) }
        }

    override var pushTokenSynced: String
        get() = secureSharedData.getString(keyPushTokenSynced, "") ?: ""
        set(value) {
            secureSharedData.edit { putString(keyPushTokenSynced, value) }
        }

    override var selectedGroupId: String?
        get() = secureSharedData.getString(keySelectedGroupID, null)
        set(value) {
            secureSharedData.edit { putString(keySelectedGroupID, value) }
        }

    override var dismissedAppVersion: Long
        get() = persistentSharedData.getLong(keyDismissedAppVersionID, 0)
        set(value) {
            persistentSharedData.edit { putLong(keyDismissedAppVersionID, value) }
        }

    override fun clear() {
        secureSharedData.edit(commit = true) { clear() }
    }


    inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String): T? {
        try {
            return enumValueOf<T>(this.getString(key, null) ?: return null)
        } catch (e: Exception) {
            return null
        }
    }

}