package app.paralino.core.db

import app.paralino.core.appPrefs
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

expect class AppPrefsProvider {
    val userSettings: Settings
    val appSettings: Settings
}

class AppPrefs(
    private val provider: AppPrefsProvider
) {

    companion object {
        lateinit var instance: AppPrefs

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
        const val keyUserKeyVersion = "prefs.user.masterKey.version"
        const val keySessionKey = "prefs.user.sessionKey"
        const val keyEmail = "prefs.profile.email"
        const val keyUserId = "prefs.profile.user_id"
        const val keyTempPassword = "prefs.user.temp_password"
        const val keyEmailConfirmed = "prefs.profile.email.confirmed"
        const val keyEmailGuest = "prefs.profile.guest.email"
        const val keyPasswordGuest = "prefs.profile.guest.password"
        const val keyPushTokenUnsynced = "prefs.device.pushToken.unsynced"
        const val keyPushTokenSynced = "prefs.device.pushToken.synced"
        const val keyLocationPushTokenUnsynced = "prefs.device.locationPushToken.unsynced"
        const val keyLocationPushTokenSynced = "prefs.device.locationPushToken.synced"
        const val keyDeviceID = "prefs.device.ID"
        const val keyDeviceIDSeed = "prefs.device.ID.seed"
        const val keySelectedGroupID = "prefs.group.selectedGroupID"
        const val keyDismissedAppVersionID = "prefs.device.dismissedAppVersion"
        const val keyTrackingBoost = "prefs.device.trackingBoostEndTimeUTC"
        const val keyPlaceActivityThrottle = "prefs.device.PlaceActivityThrottle"
        const val keyWasUserLoggedIn = "prefs.device.hadUser"
        const val keyDidEverFullyRefreshed = "prefs.device.everRefreshedData"
        const val keyDidStartedGroupOnboarding = "prefs.group.onboardingStarted"
        const val keyErrorReporting = "prefs.user.errorReporting"
        const val keyHighPerformanceDuration = "prefs.location.highPerformance.duration"
        const val keyBatterySaverPerformance = "prefs.location.batterySaver.performance"
        const val keyBalancedPerformance = "prefs.location.balanced.performance"
        const val keyBalancedHighAccuracy = "prefs.location.balanced.highAccuracy"
    }

    val isTokenValid: Boolean
        get() = sessionId.isNotBlank()

    var wasUserLoggedIn: Boolean
        get() = provider.appSettings.getBoolean(keyWasUserLoggedIn, false)
        set(value) {
            provider.appSettings.putBoolean(keyWasUserLoggedIn, value)
        }

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

    var temporaryMasterAuthKey: String
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

    var userMasterKeyVersion: Long
        get() = provider.userSettings.getLong(keyUserKeyVersion, 0)
        set(value) {
            provider.userSettings.putLong(keyUserKeyVersion, value)
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

    var locationPushTokenUnsynced: String
        get() = provider.userSettings.getString(keyLocationPushTokenUnsynced, "")
        set(value) {
            provider.userSettings.putString(keyLocationPushTokenUnsynced, value)
        }

    var locationPushTokenSynced: String
        get() = provider.userSettings.getString(keyLocationPushTokenSynced, "")
        set(value) {
            provider.userSettings.putString(keyLocationPushTokenSynced, value)
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

    var didEverFullyRefreshData: Boolean
        get() = provider.userSettings.getBoolean(keyDidEverFullyRefreshed, false)
        set(value) {
            provider.userSettings.putBoolean(keyDidEverFullyRefreshed, value)
        }

    var errorReporting: Boolean
        get() = provider.userSettings.getBoolean(keyErrorReporting, true)
        set(value) {
            provider.userSettings.putBoolean(keyErrorReporting, value)
        }

    var didStartedGroupOnboarding: Boolean
        get() = provider.userSettings.getBoolean(keyDidStartedGroupOnboarding, false)
        set(value) {
            provider.userSettings.putBoolean(keyDidStartedGroupOnboarding, value)
        }

    var locationSettingsHighPerformanceDurationType: String?
        get() = provider.userSettings.getStringOrNull(keyHighPerformanceDuration).takeIf { !it.isNullOrBlank() }
        set(value) {
            provider.userSettings.putString(keyHighPerformanceDuration, value ?: "")
        }

    var locationSettingsBatterySaverPerformanceType: String?
        get() = provider.userSettings.getStringOrNull(keyBatterySaverPerformance).takeIf { !it.isNullOrBlank() }
        set(value) {
            provider.userSettings.putString(keyBatterySaverPerformance, value ?: "")
        }

    var locationSettingsBalancedHighAccuracy: Boolean
        get() = provider.userSettings.getBoolean(keyBalancedHighAccuracy, true)
        set(value) {
            provider.userSettings.putBoolean(keyBalancedHighAccuracy, value)
        }

    var locationSettingsBalancedPerformanceType: String?
        get() = provider.userSettings.getStringOrNull(keyBalancedPerformance).takeIf { !it.isNullOrBlank() }
        set(value) {
            provider.userSettings.putString(keyBalancedPerformance, value ?: "")
        }


    fun hasGuestAccount(): Boolean {
        val emailValidation = FormValidation.Email.validate(appPrefs.guestEmail)
        val passwordValidation = FormValidation.Password.validate(appPrefs.guestPassword)
        return emailValidation.result && passwordValidation.result
    }

    fun clearPushTokens() {
        pushTokenSynced = ""
        pushTokenUnsynced = ""
        locationPushTokenSynced = ""
        locationPushTokenUnsynced = ""
    }

    fun clear() {
        provider.userSettings.clear()
    }
}