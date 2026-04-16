package app.paralino.core.models

data class AppBuildConfig constructor(
    val serverUrl: String,
    val projectId: String,
    val publicServerUrl: String,
    val billingRCApiKey: String,
    val iOSTeamId: String,
    val iOSAppGroup: String,
    val appPackageName: String,
    val appHost: AppHost,
    val isDevMode: Boolean,
    val hasPreviewFeatures: Boolean,
    val logToConsole: Boolean,
    val logErrorsToRemote: Boolean,
    val hasGoogleMaps: Boolean,
    val hasNotificationSocketService: Boolean,
    val appFlavor: AppFlavor
) {
    val endpoint = "$serverUrl/v1"

    companion object {
        lateinit var instance: AppBuildConfig
    }
}

enum class AppFlavor constructor(val key: String) {
    ANDROID_GOOGLE("android_google"),
    ANDROID_DIRECT("android_direct"),
    APPLE_IOS("ios")
}

enum class AppHost constructor(val key: String) {
    MAIN_APP("app"),
    IOS_NOTIFICATION_EXTENSION("ios_nse"),
    IOS_LOCATION_EXTENSION("ios_lse")
}
