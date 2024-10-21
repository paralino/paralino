package app.paralino.core.db

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual class AppPrefsProvider(context: Context) {
    actual val userSettings: Settings = SharedPreferencesSettings(
        EncryptedSharedPreferences(context, "paralino_prefs_user", MasterKey(context))
    )
    actual val appSettings: Settings = SharedPreferencesSettings(
        EncryptedSharedPreferences(context, "paralino_prefs_app", MasterKey(context))
    )
}