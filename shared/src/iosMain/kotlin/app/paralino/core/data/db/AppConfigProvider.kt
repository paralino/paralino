package app.paralino.core.data.db

import com.russhwolf.settings.Settings

actual class AppConfigProvider {
    actual val userSettings: Settings = KeychainSettingsFixed(
        service = "paralino_prefs_user"
    )
    actual val appSettings: Settings = KeychainSettingsFixed(
        service = "paralino_prefs_app"
    )
}
