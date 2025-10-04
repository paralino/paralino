package app.paralino.core.presentation.history

import app.paralino.core.models.data
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.stripSeconds
import kotlinx.datetime.LocalDate

class ILocationHistoryStateMapper(
    private val valueFormatter: IValueFormatter,
) {
    fun mapLocationPoints(
        entry: LocationHistoryModelForDevice,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocationHistoryPointState.Point> {
        return entry.history.data?.history
            ?.map { point ->
                val locATime = point.loc?.startTime?.isoToLocalDateTime()?.stripSeconds()
                val locBTime = point.loc?.endTime?.isoToLocalDateTime()?.stripSeconds()
                val statusATime = point.status.startTime.isoToLocalDateTime()?.stripSeconds()
                val statusBTime = point.status.endTime.isoToLocalDateTime()?.stripSeconds()

                val locationTime = valueFormatter.formatTimeRangeForLocationHistory(
                    startTimeUTC = locATime,
                    endTimeUTC = locBTime,
                    rangeStart = startDate,
                    rangeEnd = endDate,
                )

                val statusTime = if (locBTime != statusBTime) {
                    valueFormatter.formatTimeRangeForLocationHistory(
                        // using same time to just indicate last point for status update
                        startTimeUTC = statusATime.takeIf { it != locATime },
                        endTimeUTC = statusBTime,
                        rangeStart = startDate,
                        rangeEnd = endDate,
                    )
                } else {
                    null
                }

                LocationHistoryPointState.Point(
                    locationId = entry.locationId,
                    entryId = entry.entryId,
                    dateKey = entry.dateKey,
                    isCachedLocation = entry.isCachedLocation,
                    point = point,
                    locationTime = locationTime,
                    statusTime = statusTime.takeIf { it != locationTime },
                    formattedSpeed = point.loc?.spd?.let {
                        valueFormatter.getSpeedSmall(
                            metersPerSecond = it
                        )
                    },
                    formattedElevation = point.loc?.alt?.let {
                        valueFormatter.getDistanceSmall(
                            meters = it.toDouble()
                        )
                    },
                    deviceStatus = MemberDeviceStatusType.fromShortId(id = point.status.sts),
                    networkType = NetworkType.fromKey(key = point.status.net),
                    signalStrength = NetworkSignalStrength.fromLevel(level = point.status.sig)
                )
            }
            ?: emptyList()
    }
}