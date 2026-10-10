package app.paralino.core.models.settings

import app.paralino.core.MR

enum class DarkThemeStyleType(val key: String) {
    PARALINO("paralino"),
    PITCH_BLACK("pitch_black");

    val title get() = when(this) {
        PARALINO -> MR.strings.setting_appTheme_darkStyle_paralino_title
        PITCH_BLACK -> MR.strings.setting_appTheme_darkStyle_pitchBlack_title
    }

    val description get() = when(this) {
        PARALINO -> MR.strings.setting_appTheme_darkStyle_paralino_info
        PITCH_BLACK -> MR.strings.setting_appTheme_darkStyle_pitchBlack_info
    }

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}
