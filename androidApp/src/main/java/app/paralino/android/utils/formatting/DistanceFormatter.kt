package app.paralino.android.utils.formatting

import android.icu.text.DecimalFormat
import app.paralino.core.MR
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.format.DistanceUnitType

object DistanceFormatter {

    fun getDistanceSmall(
        meters: Float,
        unitType: DistanceUnitType = DistanceUnitType.METRIC,
    ): String {
        val formatter = DecimalFormat.getInstance().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return when (unitType) {
            DistanceUnitType.METRIC -> {
                if (meters < 1000) {
                    val value = formatter.apply { maximumFractionDigits = 0 }.format(meters)
                    MR.strings.placeholder_unit_meter.localizedString(value)
                } else {
                    val value = formatter.apply { maximumFractionDigits = 2 }.format(meters / 1000)
                    MR.strings.placeholder_unit_km.localizedString(value)
                }
            }
            DistanceUnitType.IMPERIAL -> {
                // TODO add imperial values
                val mile = meters / 1000 / 1.609344
                val value = formatter.format(mile)
                MR.strings.placeholder_unit_mile.localizedString(value)
            }
        }
    }

    fun getSpeedSmall(
        metersPerSecond: Float,
        unitType: DistanceUnitType = DistanceUnitType.METRIC,
    ): String {
        val kmh = metersPerSecond * 3.6
        return when (unitType) {
            DistanceUnitType.METRIC -> {
                MR.strings.placeholder_unit_speed_kmph.localizedString(kmh.toInt().toString())
            }
            DistanceUnitType.IMPERIAL -> {
                val mph = kmh / 1.609344
                MR.strings.placeholder_unit_speed_mph.localizedString(mph.toInt().toString())
            }
        }
    }

}