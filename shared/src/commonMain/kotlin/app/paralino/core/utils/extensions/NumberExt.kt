package app.paralino.core.utils.extensions

import kotlin.math.pow

fun Float.roundToPrecision(decimalPlaces: Int): Float {
    val factor = 10f.pow(decimalPlaces) // For example, 10^4 = 10000 for 4 decimal places
    return kotlin.math.ceil(this * factor) / factor
}

fun Float?.isLocationInaccurate(): Boolean {
    return (this ?: 0f) >= 200f || this == 100f
}