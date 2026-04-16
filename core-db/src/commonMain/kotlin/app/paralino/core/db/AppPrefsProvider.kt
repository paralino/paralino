package app.paralino.core.db

import com.russhwolf.settings.Settings

expect class AppPrefsProvider {
    val userSettings: Settings
    val appSettings: Settings
}