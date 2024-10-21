package app.paralino.core.provider

import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.nowUTC
import app.paralino.core.utils.format.DistanceUnitType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class IValueFormatter {

    abstract val unitType: DistanceUnitType

    abstract fun formatTimeOnly(timeUTC: LocalDateTime?): String?
    abstract fun formatNumber(number: Double, minFractions: Int, maxFractions: Int): String
    abstract fun formatBestPattern(timeUTC: LocalDateTime, pattern: String): String
    abstract fun formatWithPattern(timeUTC: LocalDateTime, pattern: String): String
    abstract fun currentTimeForApi(): String

    fun getDistanceSmall(
        meters: Double,
        unitType: DistanceUnitType? = null,
    ): String {
        return when (unitType ?: this.unitType) {
            DistanceUnitType.METRIC -> {
                if (meters.toInt() < 1000) {
                    val value = formatNumber(meters, minFractions = 0, maxFractions = 0)
                    MR.strings.placeholder_unit_meter.localizedString(value)
                } else {
                    val value = formatNumber(meters / 1000, minFractions = 0, maxFractions = 2)
                    MR.strings.placeholder_unit_km.localizedString(value)
                }
            }
            DistanceUnitType.IMPERIAL -> {
                // TODO add smaller imperial values
                val mile = meters / 1000 / 1.609344
                val value = formatNumber(mile, minFractions = 0, maxFractions = 2)
                MR.strings.placeholder_unit_mile.localizedString(value)
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
        val formattedTime = formatTimeOnly(timeUTC)

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
        val formattedTime = formatTimeOnly(timeUTC)

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
        unitType: DistanceUnitType? = null,
    ): String {
        val kmh = metersPerSecond * 3.6
        return when (unitType ?: this.unitType) {
            DistanceUnitType.METRIC -> {
                MR.strings.placeholder_unit_speed_kmph.localizedString(kmh.toInt().toString())
            }

            DistanceUnitType.IMPERIAL -> {
                val mph = kmh / 1.609344
                MR.strings.placeholder_unit_speed_mph.localizedString(mph.toInt().toString())
            }
        }
    }

    fun fromUTCtoMapSharingTime(timeUTC: String, splitTimeToOneLine: Boolean = true): String? {
        val dateTimeUTC = timeUTC.isoToLocalDateTime() ?: return null
        val local = dateTimeUTC.fromUTCtoLocal()
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val nowDate = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val sameYear = local.year == nowDate.year
        val sameDay = sameYear && local.dayOfYear == nowDate.dayOfYear
        val isTomorrow = sameYear && local.dayOfYear == now.plus(1, DateTimeUnit.DAY, timeZone)
            .toLocalDateTime(timeZone).dayOfYear

        val time = formatTimeOnly(dateTimeUTC)
        return when {
            sameDay -> time
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

    fun LocalDateTime.fromLocalToUTC(): LocalDateTime {
        return this.toInstant(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.UTC)
    }

    fun LocalDateTime.fromUTCtoLocal(): LocalDateTime {
        return this.toInstant(TimeZone.UTC).toLocalDateTime(TimeZone.currentSystemDefault())
    }
}