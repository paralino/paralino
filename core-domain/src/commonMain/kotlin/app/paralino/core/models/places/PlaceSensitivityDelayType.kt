package app.paralino.core.models.places

import app.paralino.core.resources.moko.*

enum class PlaceSensitivityDelayType(val key: String) {
    NONE("none"),
    MINIMUM("min"),
    BALANCED("mid"),
    LONG("long");

    val title
        get() = when (this) {
            NONE -> MR.strings.place_sensitivity_optionDelay1_title
            MINIMUM -> MR.strings.place_sensitivity_optionDelay2_title
            BALANCED -> MR.strings.place_sensitivity_optionDelay3_title
            LONG -> MR.strings.place_sensitivity_optionDelay4_title
        }

    val description
        get() = when (this) {
            NONE -> MR.strings.place_sensitivity_optionDelay1_info
            MINIMUM -> MR.strings.place_sensitivity_optionDelay2_info
            BALANCED -> MR.strings.place_sensitivity_optionDelay3_info
            LONG -> MR.strings.place_sensitivity_optionDelay4_info
        }

    companion object {
        val default = MINIMUM
        fun fromKey(key: String) = entries.firstOrNull { it.key == key }
    }
}