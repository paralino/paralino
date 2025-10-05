package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDate

class LocationHistoryPersonalStateMapper(
    private val valueFormatter: IValueFormatter,
    private val geofenceManager: IGeofenceManager,
    private val defaultMapper: ILocationHistoryStateMapper,
    private val clusteringService: LocationClusteringService
) {

    suspend fun mapPersonalHistory(
        space: UserPersonalSpaceModel,
        history: LocationHistoryModelForUser,
        startDate: LocalDate,
        endDate: LocalDate,
        previousState: LocationHistoryScreenState
    ): LocationHistoryScreenState {
        val user = space.user

        val knownDevices = space.devices.map {
            UserActiveLocationDeviceState.Known(
                locationId = it.id,
                deviceId = it.deviceId,
                deviceName = it.deviceNameOrNull
                    ?: it.deviceType?.toString()
                    ?: MR.strings.history_map_device_name_unknown.localizedString(),
                isSelected = it.id == previousState.activeDevices.firstOrNull { it.isSelected }?.locationId,
                isCurrent = it.deviceId == space.currentDevice?.deviceId,
                hasLocations = false
            )
        }

        val unknownDevices = history.history.filter { entry ->
            knownDevices.none { it.locationId == entry.locationId } || entry.locationId == null
        }
            .takeIf { it.isNotEmpty() }
            ?.let {
                UserActiveLocationDeviceState.Unknown(
                    ids = it.map { it.locationId }.distinct(),
                    deviceName = MR.strings.history_map_device_name_unknown.localizedString(),
                    isSelected = previousState.activeDevices.firstOrNull { it.isSelected } is UserActiveLocationDeviceState.Unknown,
                    hasLocations = false
                )
            }

        val devices = buildList {
            addAll(knownDevices)

            if (unknownDevices != null) {
                add(unknownDevices)
            }
        }.let { list ->
            // ensure always one is selected
            if (list.count { it.isSelected } != 1) {
                val firstSelected = list.firstOrNull { it.deviceId == previousState.preselectedDeviceId }
                    ?: list.firstOrNull()
                list.map {
                    when (it) {
                        is UserActiveLocationDeviceState.Known -> it.copy(isSelected = it.locationId == firstSelected?.locationId)
                        is UserActiveLocationDeviceState.Unknown -> it.copy(isSelected = it.locationId == firstSelected?.locationId)
                    }
                }
            } else {
                list
            }
        }

        val selectedKnownLocation = space.devices
            .firstOrNull { device ->
                devices
                    .filter { it.isSelected && it is UserActiveLocationDeviceState.Known }
                    .any { it.locationId == device.location?.id }
            }

        val cachedHistory = selectedKnownLocation?.location?.locationHistory?.data?.let {
            it.history
                .filter {
                    val date = it.status.tA.isoToLocalDateTime()?.date
                    if (date == null) {
                        false
                    } else {
                        date in startDate..endDate
                    }
                }
                .groupBy { it.dayKey }
                .mapNotNull { entry ->
                    entry.key?.let {
                        LocationHistoryModelForDevice(
                            locationId = selectedKnownLocation.id,
                            entryId = selectedKnownLocation.id,
                            dateKey = it,
                            historyKeyVersion = "",
                            isCachedLocation = true,
                            history = CryptoResource.Ready(LocationHistoryModel(history = entry.value))
                        )
                    }
                }
        }

        val locations = history.history
            .run {
                if (cachedHistory != null) plus(cachedHistory)
                else this
            }
            .flatMap { entry ->
                defaultMapper.mapLocationPoints(
                    entry = entry,
                    startDate = startDate,
                    endDate = endDate
                )
            }
            .sortedByDescending { it.point.status.startTime }
            .distinctBy { it.point.properID }

        val processedLocations = clusteringService.processLocations(
            points = locations,
            rangeStart = startDate,
            rangeEnd = endDate
        )

        val newState = previousState.copy(
            preselectedDeviceId = null,
            groupName = MR.strings.personalSpace_spaceTitle.localizedString(),
            groupImage = user.data?.userImage,
            isGroupOwner = true,
            userName = user.name,
            userImage = user.data?.userImage
                ?: ImageModel.emoji(Emojis.robot),
            isLoadingLocations = false,
            isLoadingData = false,
            locations = processedLocations,
            devices = devices,
            stats = null,
            planConfig = space.plan
        )

        val stats = LocationHistoryStats.calculateStats(
            state = newState,
            valueFormatter = valueFormatter,
            geofenceManager = geofenceManager,
        )

        return newState.copy(stats = stats)
    }
}