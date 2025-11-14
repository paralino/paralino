package app.paralino.core.repo.base

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.base.BackendResourceIDs

class AppConfigRepository(
    private val appPrefs: AppPrefs,
    private val appBuildConfig: AppBuildConfig
) {
    val isInBuildDevMode: Boolean get() = appBuildConfig.isDevMode
    val isInAppDevMode: Boolean get() = isInBuildDevMode || appPrefs.isDevModeEnabled
    val hasPreviewFeatures: Boolean get() = appBuildConfig.hasPreviewFeatures
    val logErrorsToRemote: Boolean get() = appBuildConfig.logErrorsToRemote
    val logToConsole: Boolean = appBuildConfig.logToConsole
    val billingRCApiKey: String get() = appBuildConfig.billingRCApiKey
    val serverUrl: String get() = appBuildConfig.serverUrl
    val endpoint: String get() = appBuildConfig.endpoint
    val projectId: String get() = appBuildConfig.projectId
    val appHost: AppHost get() = appBuildConfig.appHost
    val resourceIds: BackendResourceIDs get() = appBuildConfig.resourceIds
}