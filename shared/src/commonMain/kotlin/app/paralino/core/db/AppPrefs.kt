package app.paralino.core.db

import app.paralino.core.appPrefs
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.settings.generic.NotificationGroupingType
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.provider.MeasurementSystemType
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.datetime.Instant

expect class AppPrefsProvider {
    val userSettings: Settings
    val appSettings: Settings
}

class AppPrefs(
    private val provider: AppPrefsProvider
) {

    companion object {
        lateinit var instance: AppPrefs

        const val supportedGroupEncryptionScheme: String = "1.0"
        const val supportedAccountEncryptionScheme: String = "2.0"

        fun shouldUpgradeEncryptionScheme(
            schemeVersion: String,
            supportedEncryptionScheme: String
        ): Boolean {
            try {
                val supportedVersions = supportedEncryptionScheme.split(".")
                val versions = schemeVersion.split(".")
                return supportedVersions.first().toInt() < versions.first().toInt() || supportedVersions.get(1).toInt() < versions.get(1).toInt()
            } catch (e: Exception) {
                return false
            }
        }
        fun isEncryptionSchemeSupported(
            schemeVersion: String,
            supportedEncryptionScheme: String
        ): Boolean {
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
        const val keyEmailGuestV2 = "prefs.profile.guest.email.v2"
        const val keyPasswordGuestV2 = "prefs.profile.guest.password.v2"
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
        const val keyErrorReportingID = "prefs.user.errorReporting.ID"
        const val keyHighPerformanceDuration = "prefs.location.highPerformance.duration"
        const val keyBatterySaverPerformance = "prefs.location.batterySaver.performance"
        const val keyBatterySaverDeviceLowPower = "prefs.location.batterySaver.deviceLowPowerMode"
        const val keyBalancedPerformance = "prefs.location.balanced.performance"
        const val keyBalancedHighAccuracy = "prefs.location.balanced.highAccuracy"
        const val keyBalancedAutomaticPauseIOS = "prefs.location.ios.autoPause"
        const val keyLocationShowIndicatorIOS = "prefs.location.ios.indicator"
        const val keyWelcomeOnboarding = "prefs.device.onboarding.welcome"
        const val keyAccountWelcomeOnboarding = "prefs.device.onboarding.account"
        const val keyAccountWelcomeOnboardingNotifications = "prefs.device.onboarding.notifications"
        const val keyLoggedInAppLaunchCount = "prefs.device.appLaunchCount.account"
        const val keyShowedAppRatingTime = "prefs.user.appRating.shownTime"
        const val keyShowedAppRatingVersion = "prefs.user.appRating.appVersion"
        const val keyPreviousAppVersion = "prefs.device.appVersion.previous"
        const val keyCurrentAppVersion = "prefs.device.appVersion.current"
        const val keyMeasurementSystem = "prefs.device.measurementSystem"
        const val keyNotificationGrouping = "prefs.device.notificationGrouping"
        const val keyDeviceTheme = "prefs.device.theme"
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

    var accountRecoveryFlag: Boolean
        get() = provider.userSettings.getString(keyTempPassword, "").isNotBlank()
        set(value) {
            if (value) {
                provider.userSettings.putString(keyTempPassword, "recovery_needed")
            } else {
                provider.userSettings.putString(keyTempPassword, "")
            }
        }

    var guestEmailV1: String
        get() = provider.appSettings.getString(keyEmailGuest, "")
        set(value) {
            provider.appSettings.putString(keyEmailGuest, value)
        }

    var guestPasswordV1: String
        get() = provider.appSettings.getString(keyPasswordGuest, "")
        set(value) {
            provider.appSettings.putString(keyPasswordGuest, value)
        }

    var guestEmailV2: String
        get() = provider.appSettings.getString(keyEmailGuestV2, "")
        set(value) {
            provider.appSettings.putString(keyEmailGuestV2, value)
        }

    var guestPasswordV2: String
        get() = provider.appSettings.getString(keyPasswordGuestV2, "")
        set(value) {
            provider.appSettings.putString(keyPasswordGuestV2, value)
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

    var errorReportingID: String
        get() = provider.userSettings.getString(keyErrorReportingID, "")
        set(value) {
            provider.userSettings.putString(keyErrorReportingID, value)
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

    var locationBatterySaverRespectsDeviceLowPower: Boolean
        get() = provider.userSettings.getBoolean(keyBatterySaverDeviceLowPower, true)
        set(value) {
            provider.userSettings.putBoolean(keyBatterySaverDeviceLowPower, value)
        }

    var locationSettingsBalancedHighAccuracy: Boolean
        get() = provider.userSettings.getBoolean(keyBalancedHighAccuracy, true)
        set(value) {
            provider.userSettings.putBoolean(keyBalancedHighAccuracy, value)
        }

    var locationSettingsAutoPauseIOS: Boolean
        get() = provider.userSettings.getBoolean(keyBalancedAutomaticPauseIOS, true)
        set(value) {
            provider.userSettings.putBoolean(keyBalancedAutomaticPauseIOS, value)
        }

    var locationSettingsShowIndicatorIOS: Boolean
        get() = provider.userSettings.getBoolean(keyLocationShowIndicatorIOS, false)
        set(value) {
            provider.userSettings.putBoolean(keyLocationShowIndicatorIOS, value)
        }

    var locationSettingsBalancedPerformanceType: String?
        get() = provider.userSettings.getStringOrNull(keyBalancedPerformance).takeIf { !it.isNullOrBlank() }
        set(value) {
            provider.userSettings.putString(keyBalancedPerformance, value ?: "")
        }

    var didHaveWelcomeOnboarding: Boolean
        get() = provider.appSettings.getBoolean(keyWelcomeOnboarding, false)
        set(value) {
            provider.appSettings.putBoolean(keyWelcomeOnboarding, value)
        }

    var didHaveAccountWelcomeOnboarding: Boolean
        get() = provider.userSettings.getBoolean(keyAccountWelcomeOnboarding, false)
        set(value) {
            provider.userSettings.putBoolean(keyAccountWelcomeOnboarding, value)
        }

    var didAskForNotificationPermissions: Boolean
        get() = provider.userSettings.getBoolean(keyAccountWelcomeOnboardingNotifications, false)
        set(value) {
            provider.userSettings.putBoolean(keyAccountWelcomeOnboardingNotifications, value)
        }

    var lastShowedAppRatingDialogUTC: Instant
        get() = provider.appSettings.getLong(keyShowedAppRatingTime, 0).let { Instant.fromEpochMilliseconds(it) }
        set(value) {
            provider.appSettings.putLong(keyShowedAppRatingTime, value.toEpochMilliseconds())
        }

    var lastShowedAppRatingDialogAppVersion: Long
        get() = provider.appSettings.getLong(keyShowedAppRatingVersion, 0)
        set(value) {
            provider.appSettings.putLong(keyShowedAppRatingVersion, value)
        }

    var appLaunchedCountWhenLoggedIn: Int
        get() = provider.userSettings.getInt(keyLoggedInAppLaunchCount, 0)
        set(value) {
            provider.userSettings.putInt(keyLoggedInAppLaunchCount, value)
        }

    var previousAppVersionNumber: Long?
        get() = provider.appSettings.getLongOrNull(keyPreviousAppVersion)
        set(value) {
            if (value == null) {
                provider.appSettings.remove(keyPreviousAppVersion)
            } else {
                provider.appSettings.putLong(keyPreviousAppVersion, value)
            }
        }

    var currentAppVersionNumber: Long?
        get() = provider.appSettings.getLongOrNull(keyCurrentAppVersion)
        set(value) {
            if (value == null) {
                provider.appSettings.remove(keyCurrentAppVersion)
            } else {
                provider.appSettings.putLong(keyCurrentAppVersion, value)
            }
        }

    var measurementSystem: MeasurementSystemType?
        get() = provider.appSettings.getString(keyMeasurementSystem, "")
            .let { MeasurementSystemType.fromKey(it) }
        set(value) {
            if (value == null) {
                provider.appSettings.remove(keyMeasurementSystem)
            } else {
                provider.appSettings.putString(keyMeasurementSystem, value.key)
            }
        }

    var notificationGrouping: NotificationGroupingType
        get() = provider.appSettings.getString(keyNotificationGrouping, "")
            .let { NotificationGroupingType.fromKey(it) }
            ?: NotificationGroupingType.NONE
        set(value) {
            provider.appSettings.putString(keyNotificationGrouping, value.key)
        }


    var deviceTheme: String
        get() = provider.appSettings.getString(keyDeviceTheme, "")
        set(value) {
            provider.appSettings.putString(keyDeviceTheme, value)
        }


    fun hasGuestAccount(): Boolean {
        return hasV1GuestAccount() || hasV2GuestAccount()
    }

    fun hasV1GuestAccount(): Boolean {
        return hasGuestAccount(email = guestEmailV1, password = guestPasswordV1)
    }

    fun hasV2GuestAccount(): Boolean {
        return hasGuestAccount(email = guestEmailV2, password = guestPasswordV2)
    }

    private fun hasGuestAccount(
        email: String,
        password: String
    ): Boolean {
        val emailValidation = FormValidation.Email.validate(email)
        val passwordValidation = FormValidation.TextNotEmpty.validate(password)
        return emailValidation.result && passwordValidation.result
    }

    fun invalidateGuestAccount() {
        appPrefs.guestEmailV1 = ""
        appPrefs.guestPasswordV1 = ""
        appPrefs.guestEmailV2 = ""
        appPrefs.guestPasswordV2 = ""
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