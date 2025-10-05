package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.datetime.LocalDate

class LocationHistoryGroupStateMapper(
    private val valueFormatter: IValueFormatter,
    private val geofenceManager: IGeofenceManager,
    private val defaultMapper: ILocationHistoryStateMapper,
    private val clusteringService: LocationClusteringService,
    private val remoteConfig: RemoteConfigRepository
) {
    suspend fun mapGroupHistory(
        group: UserGroupWithMembers,
        currentMember: MemberModelWithLocations,
        history: LocationHistoryModelForUser,
        startDate: LocalDate,
        endDate: LocalDate,
        previousState: LocationHistoryScreenState
    ): LocationHistoryScreenState {
        val knownDevices = currentMember.locations.map {
            UserActiveLocationDeviceState.Known(
                locationId = it.id,
                deviceId = it.deviceId,
                deviceName = it.name,
                isSelected = it.id == previousState.activeDevices.firstOrNull { it.isSelected }?.locationId,
                isCurrent = it.id == group.currentDeviceLocation?.id,
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

        val selectedKnownLocation = currentMember.locations
            .firstOrNull { location ->
                devices
                    .filter { it.isSelected && it is UserActiveLocationDeviceState.Known }
                    .any { it.locationId == location.id }
            }

        val cachedHistory = selectedKnownLocation?.locationHistory?.data?.let {
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
            groupName = group.name,
            groupImage = group.image,
            isGroupOwner = group.isCurrentUserOwner,
            userName = currentMember.member.name,
            userImage = currentMember.member.image
                ?: ImageModel.emoji(Emojis.robot),
            isLoadingLocations = false,
            isLoadingData = false,
            locations = processedLocations,
            devices = devices,
            stats = null,
            planConfig = group.group.planConfig(remoteConfig.getConfigOnce())
        )

        val stats = LocationHistoryStats.calculateStats(
            state = newState,
            valueFormatter = valueFormatter,
            geofenceManager = geofenceManager,
        )

        return newState.copy(stats = stats)
    }
}