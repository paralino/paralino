package app.paralino.android.provider

import android.icu.text.DecimalFormat
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.text.format.DateFormat
import app.paralino.android.utils.extensions.fromUTCtoLocal
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.format.DistanceUnitType
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class ValueFormatter() : IValueFormatter() {

    override val unitType: DistanceUnitType
        get() {
            return try {
                when (LocaleData.getMeasurementSystem(ULocale.getDefault())) {
                    LocaleData.MeasurementSystem.US,
                    LocaleData.MeasurementSystem.UK -> DistanceUnitType.IMPERIAL
                    else -> DistanceUnitType.METRIC
                }
            } catch (e: Exception) {
                DistanceUnitType.METRIC
            }
        }

    override fun formatNumber(number: Double, minFractions: Int, maxFractions: Int): String {
        val formatter = DecimalFormat.getInstance().apply {
            minimumFractionDigits = minFractions
            maximumFractionDigits = maxFractions
        }
        return formatter.format(number)
    }

    override fun formatTimeOnly(timeUTC: LocalDateTime?): String? {
        val local = timeUTC?.toJavaLocalDateTime()?.fromUTCtoLocal() ?: return null
        return local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    override fun formatBestPattern(timeUTC: LocalDateTime, pattern: String): String {
        return timeUTC.toJavaLocalDateTime().fromUTCtoLocal().format(
            DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern)
            )
        )
    }

    override fun formatWithPattern(timeUTC: LocalDateTime, pattern: String): String {
        return timeUTC.toJavaLocalDateTime().fromUTCtoLocal().format(DateTimeFormatter.ofPattern(pattern))
    }

    override fun currentTimeForApi(): String {
        return nowUTC().toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}