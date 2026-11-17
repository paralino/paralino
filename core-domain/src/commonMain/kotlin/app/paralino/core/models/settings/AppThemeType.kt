package app.paralino.core.models.settings

import app.paralino.core.resources.moko.*

enum class AppThemeType(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    val title get() = when(this) {
        SYSTEM -> MR.strings.setting_appTheme_system_title
        LIGHT -> MR.strings.setting_appTheme_light_title
        DARK -> MR.strings.setting_appTheme_dark_title
    }

    val description get() = when(this) {
        SYSTEM -> MR.strings.setting_appTheme_system_info
        LIGHT -> MR.strings.setting_appTheme_light_info
        DARK -> MR.strings.setting_appTheme_dark_info
    }

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}
