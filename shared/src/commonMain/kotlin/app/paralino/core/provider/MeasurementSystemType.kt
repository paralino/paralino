package app.paralino.core.provider

enum class MeasurementSystemType constructor(val key: String) {
    METRIC("metric"),
    IMPERIAL("imperial");

    val unitsInfo: String get() = when(this) {
        METRIC -> "m, km, km/h"
        IMPERIAL -> "ft, mi, m/h"
    }

    companion object {
        fun fromKey(key: String): MeasurementSystemType? = entries.firstOrNull { it.key == key }
    }
}

enum class DistanceType constructor(val key: String) {
    ELEVATION("elevation"),
    DISTANCE("distance");
}