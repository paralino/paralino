package app.paralino.core.models.places

import app.paralino.core.resources.moko.*

enum class PlaceSensitivityLocationRefreshType(val key: String) {
    NONE("none"),
    BEFORE("before"),
    AFTER("after");

    val title
        get() = when (this) {
            NONE -> MR.strings.place_sensitivity_optionRefresh1_title
            BEFORE -> MR.strings.place_sensitivity_optionRefresh2_title
            AFTER -> MR.strings.place_sensitivity_optionRefresh3_title
        }

    val description
        get() = when (this) {
            NONE -> MR.strings.place_sensitivity_optionRefresh1_info
            BEFORE -> MR.strings.place_sensitivity_optionRefresh2_info
            AFTER -> MR.strings.place_sensitivity_optionRefresh3_info
        }

    companion object {
        val default = BEFORE
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}