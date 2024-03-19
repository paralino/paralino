package app.paralino.core.provider

import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.format.DistanceUnitType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

abstract class IValueFormatter {

    abstract val unitType: DistanceUnitType

    abstract fun getDistanceSmall(meters: Number, unitType: DistanceUnitType? = null): String
    abstract fun getLastPing(time: LocalDateTime?): String?
    abstract fun formatBestPattern(epochSeconds: Long, pattern: String): String
    abstract fun currentTimeForApi(): String

    fun getLastLocationPing(member: MemberModelWithLocations): String? {
        return getLastPing(
            member.locations
                .mapNotNull { it.locationData.data?.timeUTC?.isoToLocalDateTime() }
                .minOrNull()
        )
    }

    fun getLastStatusPing(member: MemberModelWithLocations): String? {
        return getLastPing(
            member.locations
                .mapNotNull { it.status.data?.timeUTC?.isoToLocalDateTime() }
                .minOrNull()
        )
    }

    fun getLastLocationPing(location: MemberLocation?): String? {
        return getLastPing(location?.locationData?.data?.timeUTC?.isoToLocalDateTime())
    }

    fun getLastStatusPing(location: MemberLocation?): String? {
        return getLastPing(location?.status?.data?.timeUTC?.isoToLocalDateTime())
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

        val time = formatBestPattern(local.toInstant(TimeZone.UTC).epochSeconds, "HHmm")
        return when {
            sameDay -> time
            isTomorrow -> "${MR.strings.date_name_tomorrow.localizedString()}, $time"
            sameYear -> formatBestPattern(
                epochSeconds = local.toInstant(TimeZone.UTC).epochSeconds,
                pattern = "MMMd"
            ) + ", $time"

            else -> {
                val timeFixed = if (splitTimeToOneLine) "\n$time" else ", $time"
                formatBestPattern(
                    epochSeconds = local.toInstant(TimeZone.UTC).epochSeconds,
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