package app.paralino.core.models.base

enum class ParalinoSocketConnectionType(val key: String) {
    REALTIME("realtime"),
    PERIODIC_LEVEL_1("periodic_level_1"),
    PERIODIC_LEVEL_2("periodic_level_2"),
    PERIODIC_LEVEL_3("periodic_level_3");

    companion object {
        val default = REALTIME
        fun fromKey(key: String?): ParalinoSocketConnectionType {
            return entries.find { it.key == key } ?: default
        }
    }
}
