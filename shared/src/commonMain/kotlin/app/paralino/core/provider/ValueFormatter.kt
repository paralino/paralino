package app.paralino.core.provider

import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.models.location.MemberLocation
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.format.DistanceUnitType
import kotlinx.datetime.LocalDateTime

abstract class IValueFormatter {

    abstract val unitType: DistanceUnitType

    abstract fun getDistanceSmall(meters: Float, unitType: DistanceUnitType? = null): String
    abstract fun getLastPing(time: LocalDateTime?): String?

    fun getLastPing(member: MemberModelWithLocations): String? {
        return getLastPing(
            member.locations
                .mapNotNull { it.locationData.data?.timeUTC?.isoToLocalDateTime() }
                .minOrNull()
        )
    }

    fun getLastPing(location: MemberLocation?): String? {
        return getLastPing(location?.locationData?.data?.timeUTC?.isoToLocalDateTime())
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
}