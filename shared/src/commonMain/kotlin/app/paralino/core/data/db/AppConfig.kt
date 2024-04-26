package app.paralino.core.data.db

import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.datetime.Instant

expect class AppConfigProvider {
    val userSettings: Settings
    val appSettings: Settings
}

class AppConfig(
    private val provider: AppConfigProvider
) {

    companion object {
        lateinit var instance: AppConfig

        const val supportedEncryptionScheme: String = "1.0"
        fun shouldUpgradeEncryptionScheme(schemeVersion: String): Boolean {
            try {
                val supportedVersions = supportedEncryptionScheme.split(".")
                val versions = schemeVersion.split(".")
                return supportedVersions.first().toInt() < versions.first().toInt() || supportedVersions.get(1).toInt() < versions.get(1).toInt()
            } catch (e: Exception) {
                return false
            }
        }
        fun isEncryptionSchemeSupported(schemeVersion: String): Boolean {
            try {
                val supportedVersions = supportedEncryptionScheme.split(".")
                val versions = schemeVersion.split(".")
                return supportedVersions.first().toInt() >= versions.first().toInt()
            } catch (e: Exception) {
                return false
            }
        }

        const val keyToken = "prefs.profile.token"
        const val keyUserKey = "prefs.user.masterKey"
        const val keySessionKey = "prefs.user.sessionKey"
        const val keyEmail = "prefs.profile.email"
        const val keyUserId = "prefs.profile.user_id"
        const val keyTempPassword = "prefs.user.temp_password"
        const val keyEmailConfirmed = "prefs.profile.email.confirmed"
        const val keyEmailGuest = "prefs.profile.guest.email"
        const val keyPasswordGuest = "prefs.profile.guest.password"
        const val keyPushTokenUnsynced = "prefs.device.pushToken.unsynced"
        const val keyPushTokenSynced = "prefs.device.pushToken.synced"
        const val keyDeviceID = "prefs.device.ID"
        const val keyDeviceIDSeed = "prefs.device.ID.seed"
        const val keySelectedGroupID = "prefs.group.selectedGroupID"
        const val keyDismissedAppVersionID = "prefs.device.dismissedAppVersion"
        const val keyTrackingBoost = "prefs.device.trackingBoostEndTimeUTC"
        const val keyPlaceActivityThrottle = "prefs.device.PlaceActivityThrottle"
    }

    val isTokenValid: Boolean
        get() = sessionId.isNotBlank()

    var deviceIdSeed: String
        get() = provider.appSettings.getString(keyDeviceIDSeed, "")
        set(value) {
            provider.appSettings.putString(keyDeviceIDSeed, value)
        }

    var deviceId: String
        get() = provider.userSettings.getString(keyDeviceID, "")
        set(value) {
            provider.userSettings.putString(keyDeviceID, value)
        }

    var sessionId: String
        get() = provider.userSettings.getString(keyToken, "")
        set(value) {
            provider.userSettings.putString(keyToken, value)
        }

    var email: String
        get() = provider.userSettings.getString(keyEmail, "")
        set(value) {
            provider.userSettings.putString(keyEmail, value)
        }

    var userId: String
        get() = provider.userSettings.getString(keyUserId, "")
        set(value) {
            provider.userSettings.putString(keyUserId, value)
        }

    var emailConfirmed: Boolean
        get() = provider.userSettings.getBoolean(keyEmailConfirmed, false)
        set(value) {
            provider.userSettings.putBoolean(keyEmailConfirmed, value)
        }

    var temporaryBackupPassword: String
        get() = provider.userSettings.getString(keyTempPassword, "")
        set(value) {
            provider.userSettings.putString(keyTempPassword, value)
        }

    var guestEmail: String
        get() = provider.appSettings.getString(keyEmailGuest, "")
        set(value) {
            provider.appSettings.putString(keyEmailGuest, value)
        }

    var guestPassword: String
        get() = provider.appSettings.getString(keyPasswordGuest, "")
        set(value) {
            provider.appSettings.putString(keyPasswordGuest, value)
        }

    var encryptedUserMasterKey: String
        get() = provider.userSettings.getString(keyUserKey, "")
        set(value) {
            provider.userSettings.putString(keyUserKey, value)
        }

    var sessionMasterKey: String
        get() = provider.userSettings.getString(keySessionKey, "")
        set(value) {
            provider.userSettings.putString(keySessionKey, value)
        }

    var pushTokenUnsynced: String
        get() = provider.userSettings.getString(keyPushTokenUnsynced, "")
        set(value) {
            provider.userSettings.putString(keyPushTokenUnsynced, value)
        }

    var pushTokenSynced: String
        get() = provider.userSettings.getString(keyPushTokenSynced, "")
        set(value) {
            provider.userSettings.putString(keyPushTokenSynced, value)
        }

    var selectedGroupId: String?
        get() = provider.userSettings.getStringOrNull(keySelectedGroupID)
        set(value) {
            provider.userSettings.set(keySelectedGroupID, value)
        }

    var dismissedAppVersion: Long
        get() = provider.appSettings.getLong(keyDismissedAppVersionID, 0)
        set(value) {
            provider.appSettings.putLong(keyDismissedAppVersionID, value)
        }

    var trackingBoostEndTime: Instant?
        get() = provider.userSettings.getString(keyTrackingBoost, "").isoWithOffsetToInstant()
        set(value) {
            if (value != null) {
                provider.userSettings.putString(keyTrackingBoost, value.toString())
            } else {
                provider.userSettings.remove(keyTrackingBoost)
            }
        }

    var placeActivityThrottle: Boolean
        get() = provider.userSettings.getBoolean(keyPlaceActivityThrottle, true)
        set(value) {
            provider.userSettings.putBoolean(keyPlaceActivityThrottle, value)
        }

    fun clear() {
        provider.userSettings.clear()
    }
}