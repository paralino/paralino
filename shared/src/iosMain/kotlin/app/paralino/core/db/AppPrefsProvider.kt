package app.paralino.core.db

import app.paralino.core.models.AppBuildConfig
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Security.kSecAttrAccessGroup
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

actual class AppPrefsProvider(appBuildConfig: AppBuildConfig) {

    private val _userSetting = KeychainSettingsFix(
        service = "paralino_prefs_user",
        accessGroup = "${appBuildConfig.iOSTeamId}.app.paralino.ios.SharedItems"
    )
    private val _appSettings = KeychainSettingsFix(
        service = "paralino_prefs_app",
        accessGroup = "${appBuildConfig.iOSTeamId}.app.paralino.ios.SharedItems"
    )

    @OptIn(ExperimentalSettingsImplementation::class)
    actual val userSettings: Settings = _userSetting.settings

    @OptIn(ExperimentalSettingsImplementation::class)
    actual val appSettings: Settings = _appSettings.settings
}


/**
 * Temporary solution
 * Copy of: https://github.com/russhwolf/multiplatform-settings/blob/main/multiplatform-settings/src/appleMain/kotlin/com/russhwolf/settings/KeychainSettings.kt
 * Added kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly options to saving and updating data
 * Added kSecAttrAccessGroup to specific app group to share keychain items
 * TODO remove when and if support is added directly to library
 */

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalSettingsApi::class,
    ExperimentalSettingsImplementation::class
)
private class KeychainSettingsFix(service: String, accessGroup: String) {

    @OptIn(ExperimentalNativeApi::class)
    private val serviceCleaner: Cleaner?

    @OptIn(ExperimentalNativeApi::class)
    private val groupCleaner: Cleaner?

    val settings: KeychainSettings

    init {
        val cfService = CFBridgingRetain(service)
        val cfGroup = CFBridgingRetain(accessGroup)

        settings = KeychainSettings(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to cfService,
            kSecAttrAccessGroup to cfGroup,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        )

        @OptIn(ExperimentalNativeApi::class)
        serviceCleaner = createCleaner(cfService) { CFBridgingRelease(it) }

        @OptIn(ExperimentalNativeApi::class)
        groupCleaner = createCleaner(cfGroup) { CFBridgingRelease(it) }
    }
}
