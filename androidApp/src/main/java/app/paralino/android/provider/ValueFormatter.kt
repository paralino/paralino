package app.paralino.android.provider

import android.icu.text.DecimalFormat
import android.text.format.DateFormat
import app.paralino.android.utils.extensions.fromUTCtoLocal
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.format.DistanceUnitType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class ValueFormatter(): IValueFormatter() {

    override val unitType: DistanceUnitType
        get() = DistanceUnitType.METRIC

    override fun formatNumber(number: Double, minFractions: Int, maxFractions: Int): String {
        val formatter = DecimalFormat.getInstance().apply {
            minimumFractionDigits = minFractions
            maximumFractionDigits = maxFractions
        }
        return formatter.format(number)
    }

    override fun formatTime(time: LocalDateTime?): String? {
        val local = time?.toJavaLocalDateTime()?.fromUTCtoLocal() ?: return null
        return local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    override fun formatBestPattern(epochSeconds: Long, pattern: String): String {
        val dateTime = java.time.LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
        return dateTime.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern)))
    }

    override fun currentTimeForApi(): String {
        return nowUTC().toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}