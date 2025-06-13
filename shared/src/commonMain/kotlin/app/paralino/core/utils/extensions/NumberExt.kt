package app.paralino.core.utils.extensions

import kotlin.math.pow

fun Float.roundToPrecision(decimalPlaces: Int): Float {
    val factor = 10f.pow(decimalPlaces) // For example, 10^4 = 10000 for 4 decimal places
    return (this * factor).toLong() / factor
}