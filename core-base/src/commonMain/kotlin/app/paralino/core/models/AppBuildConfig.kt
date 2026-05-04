package app.paralino.core.models

data class AppBuildConfig constructor(
    val prodServerConfig: ApiServerConfig,
    val devServerConfig: ApiServerConfig?,
    val billingRCApiKey: String?,
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
    val apiServerConfig: ApiServerConfig = if (isDevMode) {
        devServerConfig?.takeIf { it.isValid } ?: prodServerConfig
    } else {
        prodServerConfig
    }

    companion object {
        lateinit var instance: AppBuildConfig
    }
}

data class ApiServerConfig(
    val serverUrl: String,
    val projectId: String,
    val publicServerUrl: String
) {
    val endpoint = "$serverUrl/v1"
    val isValid = serverUrl.isNotBlank() && projectId.isNotBlank() && publicServerUrl.isNotBlank()
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
