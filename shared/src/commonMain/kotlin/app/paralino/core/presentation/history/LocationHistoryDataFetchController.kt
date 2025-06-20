package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.history.GetLocationHistoryUseCase
import app.paralino.core.domain.location.history.RefreshLocationHistoryUseCase
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.models.mapSuccess
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.Logr
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.stripNanoseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class LocationHistoryDataFetchController(
    val dataController: LocationHistoryViewModelDataController,
) {

    private val getLocationHistory: GetLocationHistoryUseCase = dataController.getLocationHistory
    private val refreshLocationHistory: RefreshLocationHistoryUseCase =
        dataController.refreshLocationHistory
    private val userRepo: UserRepository = dataController.userRepo
    private val userGroupRepo: UserGroupLocalDataSource = dataController.userGroupRepo
    private val valueFormatter: IValueFormatter = dataController.valueFormatter
    private val appPrefs: AppPrefs = dataController.appPrefs
    private val remoteConfig: RemoteConfigRepository = dataController.remoteConfig
    private val _uiState = dataController._uiState

    fun loadHistory(
        groupId: String,
        memberUserId: String
    ): Flow<Unit> {
        _uiState.update {
            dataController.emptyState(
                previousState = it,
                groupId = groupId,
                memberUserId = memberUserId
            )
        }

        return reload(
            dateRangeDays = _uiState.value.dateRangeDays,
            selectedDate = _uiState.value.selectedDate
        )
    }

    fun reload(
        dateRangeDays: Int,
        selectedDate: LocalDate
    ): Flow<Unit> {
        val groupId = _uiState.value.groupId
        val memberUserId = _uiState.value.memberUserId
        val job = if (groupId == UserGroup.groupPersonalDevicesId) {
            fetchPersonalLocationHistory()
        } else {
            fetchGroupLocationHistory(
                groupId = groupId,
                memberUserId = memberUserId
            )
        }

        dataController.viewModel.coroutineScope.launch {
            try {
                refreshLocationHistory.refreshLocationHistory(
                    groupId = groupId,
                    memberUserId = memberUserId,
                    locationId = null,
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate
                )
            } catch (e: Exception) {
                // TODO handle error
                logg.e(message = "Error refreshing location history: ${e.message}", t = e)
                dataController.viewModel.showSnackbar(text = e.errorString())
            }

            _uiState.update { it.copy(isRefreshingData = false) }
        }

        return job
    }

    private fun fetchPersonalLocationHistory(): Flow<Unit> {
        return combine(
            userRepo.localUser,
            userRepo.userPlans,
            getLocationHistory.getLocationHistory(
                groupId = UserGroup.groupPersonalDevicesId,
                memberUserId = appPrefs.userId,
                startDate = _uiState.value.startDate,
                endDate = _uiState.value.endDate
            )
        ) { user, plans, history ->
            // TODO add actual devices and locations
            // TODO handle user missing
            _uiState.update {
                it.copy(
                    groupName = null,
                    groupImage = null,
                    historyDaysLimit = plans.highestValidPlanConfig.personalHistoryDaysLimit,
                    isGroupOwner = true,
                    userName = user.data?.name ?: "",
                    userImage = user.data?.data?.userImage,
                    isRefreshingData = false,
                    locations = history.history.flatMap {
                        mapLocationPoints(entry = it)
                    },
                    devices = emptyList(),
                    stats = null,
                    mapFocusType = LocationHistoryMapFocusType.Stats
                )
            }
        }
            .catch { }
    }

    private fun fetchGroupLocationHistory(
        groupId: String,
        memberUserId: String,
    ): Flow<Unit> {
        return getLocationHistory.getLocationHistory(
            groupId = groupId,
            memberUserId = memberUserId,
            startDate = _uiState.value.startDate,
            endDate = _uiState.value.endDate
        ).map { history ->
            val group = userGroupRepo.getGroupWithMembersOnce(groupId = groupId)
            val currentMember = group?.memberByUserId(userId = memberUserId)
            if (group != null && currentMember != null) {

                val knownDevices = currentMember.locations.map {
                    UserActiveLocationDeviceState.Known(
                        locationId = it.id,
                        deviceId = it.deviceId,
                        deviceName = it.name,
                        isSelected = it.id == _uiState.value.devices.firstOrNull { it.isSelected }?.locationId,
                    )
                }

                val unknownDevices = history.history.filter { entry ->
                    knownDevices.none { it.locationId == entry.locationId } || entry.locationId == null
                }
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        UserActiveLocationDeviceState.Unknown(
                            ids = it.map { it.locationId }.distinct(),
                            deviceName = "Unknown",
                            isSelected = _uiState.value.devices.firstOrNull { it.isSelected } is UserActiveLocationDeviceState.Unknown,
                        )
                    }

                val devices = buildList {
                    addAll(knownDevices)

                    if (unknownDevices != null) {
                        add(unknownDevices)
                    }
                }.let { list ->
                    if (list.count { it.isSelected } != 1) {
                        val firstSelected = list.firstOrNull()
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
                                date in _uiState.value.startDate.._uiState.value.endDate
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
                        mapLocationPoints(entry = entry)
                    }
                    .sortedByDescending { it.point.status.tB }
                    .distinct()

                val newState = _uiState.value.copy(
                    groupName = group.name,
                    groupImage = group.image,
                    historyDaysLimit = group.group.historyDaysLimit(remoteConfig.getConfigOnce()),
                    isGroupOwner = group.isCurrentUserOwner,
                    userName = currentMember.member.name,
                    userImage = currentMember.member.image
                        ?: ImageModel.emoji(Emojis.robot),
                    isLoadingLocations = false,
                    locations = locations,
                    devices = devices,
                    stats = null,
                    // TODO adjust focused item
                    mapFocusType = _uiState.value.mapFocusType,
                )

                val stats = LocationHistoryStats.calculateStats(
                    state = newState,
                    valueFormatter = dataController.valueFormatter,
                    geofenceManager = dataController.geofenceManager,
                )

                _uiState.update {
                    newState.copy(stats = stats)
                }
            } else {
                // TODO handle data missing
                _uiState.update {
                    it.copy(
                        isLoadingLocations = false
                    )
                }
            }
        }
            .catch {
                Logr.d("Error loading history", it)
            }
            .flowOn(Dispatchers.IO)
    }

    private fun mapLocationPoints(entry: LocationHistoryModelForDevice): List<LocationHistoryPointState> {
        return entry.history.data?.history
            ?.map { point ->
                val locATime = point.loc?.tA?.isoToLocalDateTime()?.stripNanoseconds()
                val locBTime = point.loc?.tB?.isoToLocalDateTime()?.stripNanoseconds()
                val statusATime = point.status.tA.isoToLocalDateTime()?.stripNanoseconds()
                val statusBTime = point.status.tB.isoToLocalDateTime()?.stripNanoseconds()

                val locationTime = valueFormatter.formatTimeRangeForLocationHistory(
                    startTimeUTC = locATime,
                    endTimeUTC = locBTime,
                    rangeStart = _uiState.value.startDate,
                    rangeEnd = _uiState.value.endDate,
                )

                val statusTime = if (locBTime != statusBTime) {
                    valueFormatter.formatTimeRangeForLocationHistory(
                        // using same time to just indicate last point for status update
                        startTimeUTC = statusATime.takeIf { it != locATime },
                        endTimeUTC = statusBTime,
                        rangeStart = _uiState.value.startDate,
                        rangeEnd = _uiState.value.endDate,
                    )
                } else {
                    null
                }

                LocationHistoryPointState(
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
                    deviceStatus = MemberDeviceStatusType.fromShortId(id = point.status.stat),
                    networkType = NetworkType.fromKey(key = point.status.net),
                    signalStrength = NetworkSignalStrength.fromLevel(level = point.status.sig)
                )
            }
            ?: emptyList()
    }
}