package app.paralino.android.provider

import android.icu.text.DecimalFormat
import android.text.format.DateFormat
import app.paralino.android.utils.extensions.fromUTCtoLocal
import app.paralino.core.MR
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.format.DistanceUnitType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class ValueFormatter(): IValueFormatter() {

    override val unitType: DistanceUnitType
        get() = DistanceUnitType.METRIC

    override fun getDistanceSmall(
        meters: Float,
        unitType: DistanceUnitType?,
    ): String {
        val formatter = DecimalFormat.getInstance().apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return when (unitType ?: this.unitType) {
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

    override fun getLastPing(time: LocalDateTime?): String? {
        val local = time?.toJavaLocalDateTime()?.fromUTCtoLocal() ?: return null
        val now = java.time.LocalDateTime.now()
        val sameYear = local.year == now.year
        val sameDay = sameYear && local.dayOfYear == now.dayOfYear
        val isYesterday = sameYear && local.dayOfYear == now.minusDays(1).dayOfYear
        val time = local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

        return if (local.isAfter(java.time.LocalDateTime.now().minusMinutes(2)))
            MR.strings.group_members_last_active_status_now.localizedString()
        else if(sameDay) time
        else if(isYesterday) "${MR.strings.date_name_yesterday.localizedString()}, $time"
        else if(sameYear) local.format(
            DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(
                    Locale.getDefault(), "MMMd"))) + ", $time"
        else local.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }

}