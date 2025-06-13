package app.paralino.core.presentation.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.GetLocationHistoryUseCase
import app.paralino.core.domain.location.RefreshLocationHistoryUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            LocationHistoryScreenState(
                groupId = groupId,
                memberUserId = memberUserId,
                groupName = null,
                groupImage = null,
                userName = null,
                userImage = null,
                deviceName = null,
                isCurrentUser = memberUserId == appPrefs.userId,
                isPersonalHistory = groupId == UserGroup.groupPersonalDevicesId,
                historyDaysLimit = null,
                isGroupOwner = false,
                dateRangeDays = it.dateRangeDays,
                startDate = it.startDate,
                endDate = it.endDate,
                isDataMissing = false,
                isLoadingData = true,
                isLoadingLocations = true,
                locations = emptyList(),
                devices = emptyList(),
                selectedLocationId = null,
                selectedLocationIndex = null,
                mapFocusType = LocationHistoryMapFocusType.None,
                isSatelliteMode = it.isSatelliteMode,
                alertDialog = it.alertDialog
            )
        }

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

            _uiState.update { it.copy(isLoadingData = false) }
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
            _uiState.update {
                it.copy(
                    selectedLocationId = null,
                    groupName = null,
                    groupImage = null,
                    historyDaysLimit = plans.highestValidPlanConfig.personalHistoryDaysLimit,
                    isGroupOwner = true,
                    userName = user.data?.name ?: "",
                    userImage = user.data?.data?.userImage,
                    deviceName = "",
                    isLoadingData = false,
                    isDataMissing = user.data == null,
                    locations = history.history.flatMap {
                        it.history.data?.history
                            ?.map { mapLocationPoint(point = it) }
                            ?: emptyList()
                    },
                    devices = emptyList(),
                    selectedLocationIndex = null,
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
                val devices = currentMember.locations.map {
                    UserActiveLocationDeviceState(
                        locationId = it.id,
                        deviceId = it.deviceId,
                        deviceName = it.nameOrNull ?: "",
                    )
                }

                val selectedLocation = currentMember.locations
                    .firstOrNull { it.id == _uiState.value.selectedLocationId }
                    ?: currentMember.locations.firstOrNull()

                val cachedHistory = if (selectedLocation != null) {
                    LocationHistoryModelForDevice(
                        locationId = selectedLocation.id,
                        history = selectedLocation.locationHistory
                    )
                } else {
                    null
                }

                val locations = history.history
                    .filter { it.locationId == selectedLocation?.id || it.locationId == null }
                    .run {
                        if (cachedHistory != null) plus(cachedHistory)
                        else this
                    }
                    .flatMap {
                        it.history.data?.history
                            ?.map { mapLocationPoint(point = it) }
                            ?: emptyList()
                    }
                    .sortedByDescending { it.point.status.tB }

                _uiState.update {
                    it.copy(
                        selectedLocationId = selectedLocation?.id,
                        groupName = group.name,
                        groupImage = group.image,
                        historyDaysLimit = group.group.historyDaysLimit(remoteConfig.getConfigOnce()),
                        isGroupOwner = group.isCurrentUserOwner,
                        userName = currentMember.member.name,
                        userImage = currentMember.member.image
                            ?: ImageModel.emoji(Emojis.robot),
                        deviceName = selectedLocation?.nameOrNull,
                        isDataMissing = false,
                        isLoadingData = false,
                        locations = locations,
                        devices = devices,
                        selectedLocationIndex = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isDataMissing = false,
                        isLoadingData = false
                    )
                }
            }
        }
            .catch { }
    }

    private fun mapLocationPoint(point: LocationHistoryPointModel): LocationHistoryPointState {
        return LocationHistoryPointState(
            point = point,
            locationTime = valueFormatter.formatRelativeDateTime(point.loc?.tB?.isoToLocalDateTime()),
            statusTime = valueFormatter.formatRelativeDateTime(point.status.tB.isoToLocalDateTime()),
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
}