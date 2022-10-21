package app.paralino.android.utils.formatting

import android.content.Context
import android.icu.text.DecimalFormat
import app.paralino.android.R
import app.paralino.core.utils.format.DistanceUnitType

object DistanceFormatter {

    fun getDistanceSmall(
        context: Context,
        meters: Float,
        unitType: DistanceUnitType = DistanceUnitType.METRIC,
    ): String {
        return when (unitType) {
            DistanceUnitType.METRIC -> {
                if (meters < 1000) {
                    context.getString(R.string.placeholder_unit_meter, "$meters")
                } else {
                    val km = meters.toFloat() / 1000
                    val value = DecimalFormat.getInstance().apply {
                        minimumFractionDigits = 0
                        maximumFractionDigits = 2
                    }.format(km)
                    context.getString(R.string.placeholder_unit_km, value)
                }
            }
            DistanceUnitType.IMPERIAL -> {
                // TODO add imperial values
                val mile = meters / 1000 / 1.609344
                val value = DecimalFormat.getInstance().apply {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 2
                }.format(mile)
                context.getString(R.string.placeholder_unit_mile, value)
            }
        }
    }

    fun getSpeedSmall(
        context: Context,
        metersPerSecond: Float,
        unitType: DistanceUnitType = DistanceUnitType.METRIC,
    ): String {
        val kmh = metersPerSecond * 3.6
        return when (unitType) {
            DistanceUnitType.METRIC -> {
                context.getString(R.string.placeholder_unit_speed_kmph, kmh.toInt().toString())
            }
            DistanceUnitType.IMPERIAL -> {
                val mph = kmh / 1.609344
                context.getString(R.string.placeholder_unit_speed_mph, mph.toInt().toString())
            }
        }
    }

}