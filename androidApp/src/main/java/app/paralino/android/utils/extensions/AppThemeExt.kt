package app.paralino.android.utils.extensions

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import app.paralino.core.models.settings.AppThemeType

/**
 * On API 37+ system can manually force light/dark mode to specific apps
 * If that is enabled at the system level, these settings won't work properly and theme and colors will be messed up
 */
fun AppThemeType.applyAsNightMode(context: Context) {

    AppCompatDelegate.setDefaultNightMode(
        when (this) {
            AppThemeType.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeType.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeType.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<UiModeManager>()?.setApplicationNightMode(
            when (this) {
                AppThemeType.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                AppThemeType.LIGHT -> UiModeManager.MODE_NIGHT_NO
                AppThemeType.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        )
    }
}
