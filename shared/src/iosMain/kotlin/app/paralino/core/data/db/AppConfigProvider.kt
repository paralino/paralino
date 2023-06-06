package app.paralino.core.data.db

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

@OptIn(ExperimentalSettingsImplementation::class)
actual class AppConfigProvider {
    actual val userSettings: Settings = KeychainSettings(
        service = "paralino_prefs_user"
    )
    actual val appSettings: Settings = KeychainSettings(
        service = "paralino_prefs_user"
    )
}