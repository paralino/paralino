package app.paralino.core.db

import app.paralino.core.models.base.ParalinoSocketConnectionType
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.places.GroupPlaceSensitivityData
import app.paralino.core.presentation.common.FormValidation
import app.paralino.core.presentation.settings.generic.IOSNavigationAppType
import app.paralino.core.presentation.settings.generic.maps.MapInteractionsType
import app.paralino.core.presentation.settings.generic.maps.MapStyle
import app.paralino.core.presentation.settings.generic.maps.MapsSDKProvider
import app.paralino.core.presentation.settings.generic.NotificationGroupingType
import app.paralino.core.provider.MeasurementSystemType
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlin.time.Instant

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

        const val keySessionId = "prefs.profile.token"
        const val keySessionExpiresAt = "prefs.profile.sessionExpiration"
        const val keySessionRenewalCheckTime = "prefs.profile.sessionRenewalCheckTime"
        const val keyUserKey = "prefs.user.masterKey"
        const val keyUserKeyVersion = "prefs.user.masterKey.version"
        const val keyAppSessionKey = "prefs.user.sessionKey"
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
        const val keyIsPersonalSpaceSelected = "prefs.device.isPersonalSpaceSelected"
        const val keyDismissedAppVersionID = "prefs.device.dismissedAppVersion"
        const val keyTrackingBoost = "prefs.device.trackingBoostEndTimeUTC"
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
        const val keyLocationHistoryFeatureOnboarding = "prefs.device.onboarding.locationHistory"
        const val keyPersonalSpaceFeatureOnboarding = "prefs.device.onboarding.personalSpace"
        const val keyLoggedInAppLaunchCount = "prefs.device.appLaunchCount.account"
        const val keyShowedAppRatingTime = "prefs.user.appRating.shownTime"
        const val keyShowedAppRatingVersion = "prefs.user.appRating.appVersion"
        const val keyPreviousAppVersion = "prefs.device.appVersion.previous"
        const val keyCurrentAppVersion = "prefs.device.appVersion.current"
        const val keyMeasurementSystem = "prefs.device.measurementSystem"
        const val keyNotificationGrouping = "prefs.device.notificationGrouping"
        const val keyMapInteractions = "prefs.device.mapInteractions"
        const val keyNavigatioApp = "prefs.device.navigationApp"
        const val keyDeviceTheme = "prefs.device.theme"
        const val keyLocationHistoryPerformance = "prefs.location.history.performance"
        const val keyEmailVerificationSkipTime = "prefs.user.emailVerification.skipTime"
        const val keyPlaceSensitivityType = "prefs.device.placeSensitivity"
        const val keyDevModeEnabled = "prefs.device.devModeOverride"
        const val keyAppTappedOnReview = "prefs.device.appReviewOpened"
        const val keyPlaceSearchLocation = "prefs.device.pleaseSearchLocation"
        const val keyMapTilerToken = "prefs.device.mapsTilerToken"
        const val keyMapSDKProviderLocation = "prefs.device.mapsSDK"
        const val keyMapSDKProviderLightStyle = "prefs.device.mapsSDK.lightStyle"
        const val keyMapSDKProviderDarkStyle = "prefs.device.mapsSDK.darkStyle"
        const val keyDeviceWasEverSynced = "prefs.device.isSynced"
        const val keySelectedUnifiedPushDistributor = "prefs.device.unifiedPushDistributor"
        const val keyParalinoSocketConnectionType = "prefs.device.paralinoSocketConnectionType"
        const val keySelectedPushProvider = "prefs.device.pushProvider"
        const val keyUnifiedPushEndpointConfigUnsynced = "prefs.device.unifiedPushEndpointConfig.unsynced"
        const val keyUnifiedPushEndpointConfigSynced = "prefs.device.unifiedPushEndpointConfig.synced"
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
        get() = provider.userSettings.getString(keySessionId, "")
        set(value) {
            provider.userSettings.putString(keySessionId, value)
        }

    var sessionExpiresAt: Instant?
        get() = provider.userSettings.getString(keySessionExpiresAt, "").isoWithOffsetToInstant()
        set(value) {
            if (value != null) {
                provider.userSettings.putString(keySessionExpiresAt, value.toString())
            } else {
                provider.userSettings.remove(keySessionExpiresAt)
            }
        }

    var sessionRenewalCheckTime: Instant?
        get() = provider.userSettings.getString(keySessionRenewalCheckTime, "").isoWithOffsetToInstant()
        set(value) {
            if (value != null) {
                provider.userSettings.putString(keySessionRenewalCheckTime, value.toString())
            } else {
                provider.userSettings.remove(keySessionRenewalCheckTime)
            }
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
        get() = provider.userSettings.getString(keyAppSessionKey, "")
        set(value) {
            provider.userSettings.putString(keyAppSessionKey, value)
        }

    var wasDeviceEverSynced: Boolean
        get() = provider.userSettings.getBoolean(keyDeviceWasEverSynced, false)
        set(value) {
            provider.userSettings.putBoolean(keyDeviceWasEverSynced, value)
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

    var selectedUnifiedPushDistributor: String?
        get() = provider.userSettings.getStringOrNull(keySelectedUnifiedPushDistributor)
        set(value) {
            if (value == null) {
                provider.userSettings.remove(keySelectedUnifiedPushDistributor)
            } else {
                provider.userSettings.putString(keySelectedUnifiedPushDistributor, value)
            }
        }

    var unifiedPushEndpointConfigUnsynced: String?
        get() = provider.userSettings.getStringOrNull(keyUnifiedPushEndpointConfigUnsynced)
        set(value) {
            if (value == null) {
                provider.userSettings.remove(keyUnifiedPushEndpointConfigUnsynced)
            } else {
                provider.userSettings.putString(keyUnifiedPushEndpointConfigUnsynced, value)
            }
        }

    var unifiedPushEndpointConfigSynced: String?
        get() = provider.userSettings.getStringOrNull(keyUnifiedPushEndpointConfigSynced)
        set(value) {
            if (value == null) {
                provider.userSettings.remove(keyUnifiedPushEndpointConfigSynced)
            } else {
                provider.userSettings.putString(keyUnifiedPushEndpointConfigSynced, value)
            }
        }

    var notificationPushProvider: PushProvider?
        get() = provider.userSettings.getStringOrNull(keySelectedPushProvider)?.let {
            PushProvider.fromKey(key = it)
        }
        set(value) {
            if (value == null) {
                provider.userSettings.remove(keySelectedPushProvider)
            } else {
                provider.userSettings.putString(keySelectedPushProvider, value.key)
            }
        }

    var paralinoSocketConnectionType: ParalinoSocketConnectionType
        get() = provider.userSettings.getStringOrNull(keyParalinoSocketConnectionType).let { ParalinoSocketConnectionType.fromKey(it) }
        set(value) {
            provider.userSettings.putString(keyParalinoSocketConnectionType, value.key)
        }

    var selectedGroupId: String?
        get() = provider.userSettings.getStringOrNull(keySelectedGroupID)
        set(value) {
            if (value == null) {
                provider.userSettings.remove(keySelectedGroupID)
            } else {
                provider.userSettings.putString(keySelectedGroupID, value)
            }
        }

    var isPersonalSpaceSelected: Boolean
        get() = provider.userSettings.getBoolean(keyIsPersonalSpaceSelected, false)
        set(value) {
            provider.userSettings.putBoolean(keyIsPersonalSpaceSelected, value)
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

    var locationSettingsHistoryPerformanceType: String?
        get() = provider.userSettings.getStringOrNull(keyLocationHistoryPerformance).takeIf { !it.isNullOrBlank() }
        set(value) {
            provider.userSettings.putString(keyLocationHistoryPerformance, value ?: "")
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

    var didHaveLocationHistoryIntroOnboarding: Boolean
        get() = provider.userSettings.getBoolean(keyLocationHistoryFeatureOnboarding, false)
        set(value) {
            provider.userSettings.putBoolean(keyLocationHistoryFeatureOnboarding, value)
        }

    var didHavePersonalSpaceIntroOnboarding: Boolean
        get() = provider.userSettings.getBoolean(keyPersonalSpaceFeatureOnboarding, false)
        set(value) {
            provider.userSettings.putBoolean(keyPersonalSpaceFeatureOnboarding, value)
        }

    var didAskForNotificationPermissions: Boolean
        get() = provider.userSettings.getBoolean(keyAccountWelcomeOnboardingNotifications, false)
        set(value) {
            provider.userSettings.putBoolean(keyAccountWelcomeOnboardingNotifications, value)
        }

    var emailVerificationSkipTime: Instant?
        get() = provider.userSettings.getString(keyEmailVerificationSkipTime, "").isoWithOffsetToInstant()
        set(value) {
            if (value != null) {
                provider.userSettings.putString(keyEmailVerificationSkipTime, value.toString())
            } else {
                provider.userSettings.remove(keyEmailVerificationSkipTime)
            }
        }

    var lastShowedAppRatingDialogUTC: Instant
        get() = provider.userSettings.getLong(keyShowedAppRatingTime, 0).let { Instant.fromEpochMilliseconds(it) }
        set(value) {
            provider.userSettings.putLong(keyShowedAppRatingTime, value.toEpochMilliseconds())
        }

    var lastShowedAppRatingDialogAppVersion: Long
        get() = provider.userSettings.getLong(keyShowedAppRatingVersion, 0)
        set(value) {
            provider.userSettings.putLong(keyShowedAppRatingVersion, value)
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

    var mapInteractions: MapInteractionsType
        get() = provider.appSettings.getString(keyMapInteractions, "")
            .let { MapInteractionsType.fromKey(it) }
            ?: MapInteractionsType.DEVICE_DEFAULT
        set(value) {
            provider.appSettings.putString(keyMapInteractions, value.key)
        }

    var iosNavigationApp: IOSNavigationAppType
        get() {
            val value = provider.appSettings.getString(keyNavigatioApp, "")
                .let { IOSNavigationAppType.fromKey(it) }

            if (value != null) {
                return value
            } else {
                val default = IOSNavigationAppType.default
                iosNavigationApp = default
                return default
            }
        }
        set(value) {
            provider.appSettings.putString(keyNavigatioApp, value.key)
        }


    var deviceTheme: String
        get() = provider.appSettings.getString(keyDeviceTheme, "")
        set(value) {
            provider.appSettings.putString(keyDeviceTheme, value)
        }

    fun setPlaceSensitivityForPlace(placeId: String, sensitivity: GroupPlaceSensitivityData?) {
        if (sensitivity != null) {
            val model = BaseRemoteModule.formatter.encodeToString(sensitivity)
            provider.userSettings.putString("${keyPlaceSensitivityType}_${placeId}", model)
        } else {
            provider.userSettings.remove("${keyPlaceSensitivityType}_${placeId}")
        }
    }

    fun getPlaceSensitivityForPlace(placeId: String): GroupPlaceSensitivityData? {
        return provider.userSettings.getStringOrNull("${keyPlaceSensitivityType}_${placeId}")?.let {
            try {
                BaseRemoteModule.formatter.decodeFromString(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    var isDevModeEnabled: Boolean
        get() = provider.userSettings.getBoolean(keyDevModeEnabled, false)
        set(value) {
            provider.userSettings.putBoolean(keyDevModeEnabled, value)
        }

    var wasAppReviewOpened: Boolean
        get() = provider.userSettings.getBoolean(keyAppTappedOnReview, false)
        set(value) {
            provider.userSettings.putBoolean(keyAppTappedOnReview, value)
        }

    var sendLocationWithPlaceSearch: Boolean
        get() = provider.userSettings.getBoolean(keyPlaceSearchLocation, true)
        set(value) {
            provider.userSettings.putBoolean(keyPlaceSearchLocation, value)
        }

    fun getAndroidMapSDKProviderOrDefault(
        hasGoogleMaps: Boolean,
        isPremiumPlan: Boolean,
        mapTilerToken: String?
    ): MapsSDKProvider {
        val settingKey = provider.userSettings.getString(keyMapSDKProviderLocation, "")
        val lightStyleKey = provider.userSettings.getString(keyMapSDKProviderLightStyle, "")
        val darkStyleKey = provider.userSettings.getString(keyMapSDKProviderDarkStyle, "")

        val lightStyle = MapStyle.lightStyleFromKey(lightStyleKey)
        val darkStyle = MapStyle.darkStyleFromKey(darkStyleKey)

        val default = if (hasGoogleMaps) {
            MapsSDKProvider.GOOGLE
        } else if (isPremiumPlan && !mapTilerToken.isNullOrBlank()) {
            MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM(mapTilerToken = mapTilerToken)
        } else {
            MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE()
        }

        val setting = MapsSDKProvider.fromKey(
            key = settingKey,
            mapTilerToken = mapTilerToken ?: "",
            lightStyle = lightStyle,
            darkStyle = darkStyle
        ) ?: default

        return if (!isPremiumPlan && setting.isPremium) {
            MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE(
                lightStyle = lightStyle,
                darkStyle = darkStyle
            )
        } else if (setting is MapsSDKProvider.MAPLIBRE.MAPLIBRE_PREMIUM && mapTilerToken.isNullOrBlank()) {
            MapsSDKProvider.MAPLIBRE.MAPLIBRE_FREE(
                lightStyle = lightStyle,
                darkStyle = darkStyle
            )
        } else if (!hasGoogleMaps && setting == MapsSDKProvider.GOOGLE) {
            default
        } else {
            setting
        }
    }

    fun setAndroidMapSDKProvider(maps: MapsSDKProvider) {
        provider.userSettings.putString(keyMapSDKProviderLocation, maps.key)
        if (maps is MapsSDKProvider.MAPLIBRE) {
            provider.userSettings.putString(keyMapSDKProviderLightStyle, maps.lightStyle.key)
            provider.userSettings.putString(keyMapSDKProviderDarkStyle, maps.darkStyle.key)
        }
    }

    var mapTilerToken: String?
        get() = provider.userSettings.getStringOrNull(keyMapTilerToken)
        set(value) {
            if (value == null) {
                provider.userSettings.remove(keyMapTilerToken)
            } else {
                provider.userSettings.putString(keyMapTilerToken, value)
            }
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
        guestEmailV1 = ""
        guestPasswordV1 = ""
        guestEmailV2 = ""
        guestPasswordV2 = ""
    }

    fun clearPushTokens() {
        pushTokenSynced = ""
        pushTokenUnsynced = ""
        locationPushTokenSynced = ""
        locationPushTokenUnsynced = ""
        unifiedPushEndpointConfigUnsynced = null
        unifiedPushEndpointConfigSynced = null
    }

    fun clear() {
        provider.userSettings.clear()
    }
}