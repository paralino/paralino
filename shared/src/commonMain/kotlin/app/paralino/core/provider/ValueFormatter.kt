package app.paralino.core.provider

import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class IValueFormatter {

    abstract val measurementSystem: MeasurementSystemType
    abstract val deviceDefaultMeasurementSystem: MeasurementSystemType
    private val kmInMile = 1.609344

    abstract fun formatTimeOnly(timeUTC: LocalDateTime?, showSeconds: Boolean): String?
    abstract fun formatNumber(number: Double, minFractions: Int, maxFractions: Int): String
    abstract fun formatBestPattern(timeUTC: LocalDateTime, pattern: String): String
    abstract fun formatWithPattern(timeUTC: LocalDateTime, pattern: String): String
    abstract fun currentTimeForApi(): String

    fun getDistanceSmall(
        meters: Double,
        measurementSystem: MeasurementSystemType? = null,
    ): String {
        return when (measurementSystem ?: this.measurementSystem) {
            MeasurementSystemType.METRIC -> {
                if (meters.toInt() < 1000) {
                    val value = formatNumber(meters, minFractions = 0, maxFractions = 0)
                    MR.strings.placeholder_unit_meter.localizedString(value)
                } else {
                    val value = formatNumber(meters / 1000, minFractions = 0, maxFractions = 2)
                    MR.strings.placeholder_unit_km.localizedString(value)
                }
            }
            MeasurementSystemType.IMPERIAL -> {
                if (meters / 1000 < kmInMile / 10) { // Less than 1/10 mile
                    val feet = meters * 3.28084
                    val value = formatNumber(feet, minFractions = 0, maxFractions = 0)
                    MR.strings.placeholder_unit_feet.localizedString(value)
                } else {
                    val miles = meters / 1000 / kmInMile
                    val value = formatNumber(miles, minFractions = 0, maxFractions = 2)
                    MR.strings.placeholder_unit_mile.localizedString(value)
                }
            }
        }
    }

    fun getLastLocationPing(member: MemberModelWithLocations): String? {
        return formatRelativeDateTime(
            member.locations
                .mapNotNull { it.locationData.data?.timeUTC?.isoToLocalDateTime() }
                .minOrNull()
        )
    }

    fun getLastStatusPing(member: MemberModelWithLocations): String? {
        return formatRelativeDateTime(
            member.locations
                .mapNotNull { it.status.data?.timeUTC?.isoToLocalDateTime() }
                .minOrNull()
        )
    }

    fun getLastLocationPing(location: MemberLocation?): String? {
        return formatRelativeDateTime(location?.locationData?.data?.timeUTC?.isoToLocalDateTime())
    }

    fun getLastStatusPing(location: MemberLocation?): String? {
        return formatRelativeDateTime(location?.status?.data?.timeUTC?.isoToLocalDateTime())
    }

    fun formatRelativeDateTime(timeUTC: LocalDateTime?): String? {
        val local = timeUTC?.fromUTCtoLocal() ?: return null
        val timeInstant = timeUTC.toInstant(TimeZone.UTC)
        val now = nowUTC()
        val nowTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val sameYear = local.year == nowTime.year
        val sameDay = sameYear && local.dayOfYear == nowTime.dayOfYear
        val isYesterday = sameYear && local.dayOfYear == now.minus(1.days)
            .toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear
        val formattedTime = formatTimeOnly(timeUTC = timeUTC, showSeconds = false)

        return when {
            timeInstant > now.minus(20.seconds) -> {
                MR.strings.group_members_last_active_status_now.localizedString()
            }

            timeInstant > now.minus(1.minutes) -> {
                MR.strings.group_members_last_active_status_recently_1.localizedString()
            }

            timeInstant > now.minus(2.minutes) -> {
                MR.strings.group_members_last_active_status_recently_2.localizedString()
            }

            sameDay -> {
                "${MR.strings.date_name_today.localizedString()}, $formattedTime"
            }

            isYesterday -> {
                "${MR.strings.date_name_yesterday.localizedString()}, $formattedTime"
            }

            sameYear -> {
                formatBestPattern(
                    timeUTC = timeUTC,
                    pattern = "MMMMd"
                ) + ", $formattedTime"
            }

            else -> {
                formatBestPattern(
                    timeUTC = timeUTC,
                    pattern = "MMMMdyyyy"
                ).let { "**$it**" } + ", $formattedTime"
            }
        }
    }

    fun formatSimpleDateTime(
        timeUTC: LocalDateTime?,
        optionalYear: Boolean = true,
        optionalMonth: Boolean = true,
        showTodayInfo: Boolean = true
    ): String? {
        val local = timeUTC?.fromUTCtoLocal() ?: return null
        val nowTime = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault())
        val sameYear = local.year == nowTime.year
        val sameDay = sameYear && local.dayOfYear == nowTime.dayOfYear
        val formattedTime = formatTimeOnly(timeUTC = timeUTC, showSeconds = false)

        return when {
            sameDay && optionalMonth -> {
                if (showTodayInfo) {
                    "${MR.strings.date_name_today.localizedString()}, $formattedTime"
                } else {
                    formattedTime
                }
            }

            optionalYear && sameYear -> {
                formatBestPattern(
                    timeUTC = timeUTC,
                    pattern = "MMMMd"
                ) + ", $formattedTime"
            }

            else -> {
                formatBestPattern(
                    timeUTC = timeUTC,
                    pattern = "MMMMdyyyy"
                ) + ", $formattedTime"
            }
        }
    }

    fun getSpeedSmall(
        metersPerSecond: Float,
        measurementSystem: MeasurementSystemType? = null,
    ): String {
        val kmh = metersPerSecond * 3.6
        return when (measurementSystem ?: this.measurementSystem) {
            MeasurementSystemType.METRIC -> {
                MR.strings.placeholder_unit_speed_kmph.localizedString(kmh.toInt().toString())
            }

            MeasurementSystemType.IMPERIAL -> {
                val mph = kmh / kmInMile
                MR.strings.placeholder_unit_speed_mph.localizedString(mph.toInt().toString())
            }
        }
    }

    fun fromUTCtoMapSharingTime(timeUTC: String, splitTimeToOneLine: Boolean = true): String? {
        val dateTimeUTC = timeUTC.isoToLocalDateTime() ?: return null
        val timeZone = TimeZone.currentSystemDefault()
        val local = dateTimeUTC.fromUTCtoLocal(timeZone)
        val now = nowUTC()
        val nowDate = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val sameYear = local.year == nowDate.year
        val sameDay = sameYear && local.dayOfYear == nowDate.dayOfYear
        val isTomorrow = sameYear && local.dayOfYear == now.plus(1, DateTimeUnit.DAY, timeZone)
            .toLocalDateTime(timeZone).dayOfYear

        val time = formatTimeOnly(timeUTC = dateTimeUTC, showSeconds = false)
        return when {
            sameDay -> "${MR.strings.date_name_today.localizedString()}, $time"
            isTomorrow -> "${MR.strings.date_name_tomorrow.localizedString()}, $time"
            sameYear -> formatBestPattern(
                timeUTC = dateTimeUTC,
                pattern = "MMMd"
            ) + ", $time"

            else -> {
                val timeFixed = if (splitTimeToOneLine) "\n$time" else ", $time"
                formatBestPattern(
                    timeUTC = dateTimeUTC,
                    pattern = "MMMdyyyy"
                ) + timeFixed
            }
        }
    }

    fun formatTimeRangeForLocationHistory(
        startTimeUTC: LocalDateTime?,
        endTimeUTC: LocalDateTime?,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        nowDate: LocalDateTime = nowUTC().toLocalDateTime(timeZone),
        yesterday: LocalDate = nowDate.date.minus(DatePeriod(days = 1))
    ): String? {
        if (startTimeUTC == null && endTimeUTC == null) return null

        val startTimeCorrect = startTimeUTC ?: endTimeUTC ?: return null
        val endTimeCorrect = endTimeUTC ?: startTimeUTC ?: return null

        val currentYear = nowDate.year

        val localStart = startTimeCorrect.fromUTCtoLocal(timeZone = timeZone)
        val localEnd = endTimeCorrect.fromUTCtoLocal(timeZone = timeZone)

        // Determine if we should show years based on context
        val shouldShowYear = shouldShowYearInContext(
            itemStartYear = localStart.year,
            itemEndYear = localEnd.year,
            currentYear = currentYear,
            rangeStartYear = rangeStart.year,
            rangeEndYear = rangeEnd.year
        )

        // Check if start and end are exactly the same
        if (localStart == localEnd) {
            return formatSingleDateTime(
                dateTime = localStart,
                timeUTC = startTimeCorrect,
                nowDate = nowDate.date,
                yesterday = yesterday,
                shouldShowYear = shouldShowYear
            )
        }

        // Check if same day (different times)
        val isSameDay = localStart.date == localEnd.date

        return if (isSameDay) {
            // Same day, different times: "June 3, 2025 12:14:23 - 12:16:20"
            val datePart = formatDatePart(
                date = localStart,
                nowDate = nowDate.date,
                yesterday = yesterday,
                shouldShowYear = shouldShowYear
            )
            val startTime = formatTimeOnly(timeUTC = startTimeCorrect, showSeconds = false)
            val endTime = formatTimeOnly(timeUTC = endTimeCorrect, showSeconds = false)
            val time = listOf(startTime, endTime).distinct().joinToString(separator = " - ")
            "$datePart $time"
        } else {
            // Different days: "June 3, 2025 12:14:23 - June 4, 2025 12:16:20"
            val startPart = formatDateTimePart(
                dateTime = localStart,
                timeUTC = startTimeCorrect,
                nowDate = nowDate.date,
                yesterday = yesterday,
                shouldShowYear = shouldShowYear
            )
            val endPart = formatDateTimePart(
                dateTime = localEnd,
                timeUTC = endTimeCorrect,
                nowDate = nowDate.date,
                yesterday = yesterday,
                shouldShowYear = shouldShowYear
            )
            "$startPart - $endPart"
        }
    }

    private fun shouldShowYearInContext(
        itemStartYear: Int,
        itemEndYear: Int,
        currentYear: Int,
        rangeStartYear: Int,
        rangeEndYear: Int
    ): Boolean {
        // Show year if:
        // 1. Any item year is different from current year, OR
        // 2. Range spans different years from current year, OR
        // 3. Multiple different years are involved
        val allYears = setOf(itemStartYear, itemEndYear, currentYear, rangeStartYear, rangeEndYear)

        return allYears.size > 1 ||
                rangeStartYear != currentYear ||
                rangeEndYear != currentYear ||
                itemStartYear != currentYear ||
                itemEndYear != currentYear
    }

    private fun formatSingleDateTime(
        dateTime: LocalDateTime,
        timeUTC: LocalDateTime,
        nowDate: LocalDate,
        yesterday: LocalDate,
        shouldShowYear: Boolean
    ): String {
        val timeStr = formatTimeOnly(timeUTC = timeUTC, showSeconds = false)

        return when {
            dateTime.date == nowDate -> MR.strings.date_name_today.localizedString() + " $timeStr"
            dateTime.date == yesterday -> MR.strings.date_name_yesterday.localizedString() + " $timeStr"
            else -> {
                val datePart = formatDatePart(
                    date = dateTime,
                    nowDate = nowDate,
                    yesterday = yesterday,
                    shouldShowYear = shouldShowYear
                )
                "$datePart $timeStr"
            }
        }
    }

    private fun formatDateTimePart(
        dateTime: LocalDateTime,
        timeUTC: LocalDateTime,
        nowDate: LocalDate,
        yesterday: LocalDate,
        shouldShowYear: Boolean
    ): String {
        val timeStr = formatTimeOnly(timeUTC = timeUTC, showSeconds = false)

        return when {
            dateTime.date == nowDate -> MR.strings.date_name_today.localizedString() + " $timeStr"
            dateTime.date == yesterday -> MR.strings.date_name_yesterday.localizedString() + " $timeStr"
            else -> {
                val datePart = formatDatePart(
                    date = dateTime,
                    nowDate = nowDate,
                    yesterday = yesterday,
                    shouldShowYear = shouldShowYear
                )
                "$datePart $timeStr"
            }
        }
    }

    private fun formatDatePart(
        date: LocalDateTime,
        nowDate: LocalDate,
        yesterday: LocalDate,
        shouldShowYear: Boolean
    ): String {
        return when {
            date.date == nowDate -> MR.strings.date_name_today.localizedString()
            date.date == yesterday -> MR.strings.date_name_yesterday.localizedString()
            else -> {
                val pattern = if (shouldShowYear) "MMMd, yyyy" else "MMMd"
                formatBestPattern(
                    timeUTC = date,
                    pattern = pattern
                )
            }
        }
    }

    fun LocalDateTime.fromLocalToUTC(): LocalDateTime {
        return this.toInstant(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.UTC)
    }

    fun LocalDateTime.fromUTCtoLocal(
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDateTime {
        return this.toInstant(TimeZone.UTC).toLocalDateTime(timeZone)
    }
}