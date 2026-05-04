package app.paralino.core.presentation.history

import app.paralino.core.MR
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.location.UserPersonalSpaceModel
import app.paralino.core.provider.localizedString
import app.paralino.core.utils.extensions.isoToLocalDateTimeAtTimezone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

class LocationHistoryPersonalStateMapper(
    private val defaultMapper: ILocationHistoryStateMapper,
    private val _uiState: MutableStateFlow<LocationHistoryScreenState>
) {

    suspend fun mapPersonalHistory(
        space: UserPersonalSpaceModel?,
        history: LocationHistoryModelForUser,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (space == null) {
            // TODO handle data missing
            _uiState.update {
                it.copy(
                    isLoadingLocations = false
                )
            }
            return
        }

        val user = space.user

        val knownDevices = space.devices.map {
            UserActiveLocationDeviceState.Known(
                locationId = it.id,
                deviceId = it.deviceId,
                deviceName = it.deviceNameOrNull
                    ?: it.deviceType?.toString()
                    ?: MR.strings.history_map_device_name_unknown.localizedString(),
                isSelected = it.id == _uiState.value.activeDevices.firstOrNull { it.isSelected }?.locationId,
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
                    isSelected = _uiState.value.activeDevices.firstOrNull { it.isSelected } is UserActiveLocationDeviceState.Unknown,
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
                val firstSelected =
                    list.firstOrNull { it.deviceId == _uiState.value.preselectedDeviceId }
                        ?: list.firstOrNull { it.isCurrent }
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

        val timeStart = startDate.atTime(0, 0)
        val timeEndExclusive = endDate
            .plus(DatePeriod(days = 1))
            .atTime(0, 0)
        val localTimezone = TimeZone.currentSystemDefault()

        val cachedHistory = space.devices
            .flatMap { device ->
                device.location?.locationHistory?.data?.let {
                    it.history
                        .filter {
                            val date = it.status.tA.isoToLocalDateTimeAtTimezone(timezone = localTimezone)
                            if (date == null) {
                                false
                            } else {
                                date >= timeStart && date < timeEndExclusive
                            }
                        }
                        .groupBy { it.dayKey }
                        .mapNotNull { entry ->
                            entry.key?.let {
                                LocationHistoryModelForDevice(
                                    locationId = device.id,
                                    entryId = device.id,
                                    dateKey = it,
                                    historyKeyVersion = "",
                                    isCachedLocation = true,
                                    history = CryptoResource.Ready(LocationHistoryModel(history = entry.value))
                                )
                            }
                        }
                } ?: emptyList()
            }

        logg.v("history_fetch", "History ready, start UI update")
        defaultMapper.mapFinalState(
            startDate = startDate,
            endDate = endDate,
            history = history,
            cachedHistory = cachedHistory,
            groupName = MR.strings.personalSpace_spaceTitle.localizedString(),
            groupImage = user.data?.userImage,
            isGroupOwner = true,
            userName = user.name,
            userImage = user.data?.userImage,
            devices = devices,
            planConfig = space.plan
        )
    }
}