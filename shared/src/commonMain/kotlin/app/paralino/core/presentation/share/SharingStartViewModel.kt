package app.paralino.core.presentation.share

import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.IConnectionStatusProvider
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.SharingDurationPreset
import app.paralino.core.utils.exceptions.TimeoutException
import app.paralino.core.utils.poll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharingStartViewModel(
    groupId: String?,
    private val groupRepo: GroupRepository,
    private val sharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider,
    private val connectionStatus: IConnectionStatusProvider,
    private val registerDevice: RegisterDeviceForSharingUseCase,
    private val userMemberIdUseCase: UserMemberIdUseCase,
    private val locationRepo: LocationLocalDataSource
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow(
            SharingStartScreenState(
                groupState = if (groupId.isNullOrBlank())
                    Payload.Success(null)
                else Payload.Loading
            )
        )
    val uiState: Flow<SharingStartScreenState> = _uiState

    private val currentGroup = MutableStateFlow<UserGroup?>(null)

    private val _eventChannel = Channel<SharingStartScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {

        groupId?.let { setSelectedGroup(it) }

        combine(
            locationStatusProvider.status,
            connectionStatus.status,
            currentGroup.flatMapMerge {
                if(it == null)
                    flowOf(0)
                else
                    locationRepo.getLocationCountForMember(userMemberIdUseCase.getCurrentUserMemberIdForGroup(it.id)!!)
            }
        ) { locationStatus, connection, locationCount ->
            _uiState.update {
                it.copy(
                    deviceName = userMemberIdUseCase.getDefaultDeviceName(groupId),
                    devicesCount = locationCount,
                    errors = buildList {
                        addAll(locationStatus.getPossibleErrors(it.isLongTerm))

                        if (!connection.isOnline) {
                            add(ConditionErrorType.NETWORK)
                        }
                    },
                    warnings = buildList {
                        if (!locationStatus.isNotificationPermissionAllowed) {
                            add(ConditionErrorType.PERMISSION_NOTIFICATIONS)
                        }
                    }
                )
            }
        }.launchIn(coroutineScope)
    }

    suspend fun getAllGroupItems(): List<GroupItemState> {
        return getAllGroups().map { GroupItemState(it) }
    }

    private suspend fun getAllGroups(): List<UserGroup> {
        return groupRepo.getAllGroupsLocally().firstOrNull() ?: emptyList()
    }

    fun setSelectedGroup(groupId: String) {
        coroutineScope.launch {
            getAllGroups().firstOrNull { it.id == groupId }?.let { group ->
                setSelectedGroup(group)
            } ?: let {
                currentGroup.update { null }
                _uiState.update { it.copy(groupState = Payload.Success(null)) }
            }
        }
    }

    private fun setSelectedGroup(group: UserGroup) {
        val groupItem = GroupItemState(group)
        currentGroup.update { group }
        _uiState.update {
            it.copy(
                groupState = Payload.Success(groupItem),
                devicesLimit = group.planType.devicesLimit
            )
        }
    }

    fun startSharing() {

        if (getErrors(forLongTerm = _uiState.value.isLongTerm).isEmpty() && _uiState.value.canShare) {
            proceedWithSharing()
            return
        }

        _uiState.value.groupState.data ?: let {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.SelectGroupInfo)
            }
            return
        }

        if(_uiState.value.groupState.data?.uiType == GroupItemState.GroupUiType.UNVERIFIED) {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.UnverifiedGroup)
            }
        } else if (getErrors(forLongTerm = false).isEmpty() && _uiState.value.isLongTerm) {
            // only background location permission is missing
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.MissingBackgroundPermission)
            }
        } else {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.LocationStatusInsufficient)
            }
        }
    }

    private fun proceedWithSharing() {
        val group = _uiState.value.groupState.data ?: let {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.SelectGroupInfo)
            }
            return
        }

        coroutineScope.launch {
            _uiState.update { it.copy(loadingAction = true) }

            if(_uiState.value.devicesCount >= _uiState.value.devicesLimit && _uiState.value.devicesLimit > 1 && !registerDevice.isDeviceAlreadyRegistered(groupId = group.id)) {
                coroutineScope.launch {
                    _eventChannel.send(SharingStartScreenEvent.DevicesLimitReached(_uiState.value.devicesLimit))
                }
                _uiState.update { it.copy(loadingAction = false) }
                return@launch
            }

            val contract = LocationSharingContract(
                groupId = group.id,
                endTimeUTC = LocationSharingContract.forDuration(_uiState.value.sharingTimeMillis),
                shareBattery = _uiState.value.shareBattery,
                shareSpeed = _uiState.value.shareSpeed,
                shareElevation = _uiState.value.shareElevation,
                shareNetwork = _uiState.value.shareNetwork,
                approximateLocationInMeters = _uiState.value.approximateLocationMeters.takeIf { _uiState.value.shareApproximateLocation }
            )

            try {
                registerDevice.registerDevice(
                    groupId = group.id,
                    deviceName = _uiState.value.deviceName,
                    contract = contract,
                    notifyOthers = _uiState.value.notifyOthers
                )
            } catch (e: Exception) {
                logg.e("Unable to register device", e)
                _uiState.update { it.copy(loadingAction = false) }
                _eventChannel.send(SharingStartScreenEvent.CanNotRegisterDevice)
                return@launch
            }

            logg.tag("sharing_start").d("Device registered - register sharing contract")
            sharingManager.startSharing(contract)

            logg.tag("sharing_start").d("Check sharing contract validity")
            val active = try {
                poll(
                    firstDelayMillis = 500,
                    delay = 1000,
                    timeout = 3000
                ) {
                    sharingManager.isSharingActiveForGroup(group.id).takeIf { it }
                }
            } catch (e: TimeoutException) {
                logg.e("Sharing timeout", e)
                false
            }

            logg.tag("sharing_start").d("Sharing location setup finished")

            _uiState.update { it.copy(loadingAction = false) }
            coroutineScope.launch {
                if (active) {
                    _eventChannel.send(SharingStartScreenEvent.Started)
                } else {
                    _eventChannel.send(SharingStartScreenEvent.Error(Exception("Unable to start sharing")))
                }
            }
        }

    }

    fun turnOnApproximateLocationSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareApproximateLocation = state)
        }
    }

    fun turnOnBatterySharing(state: Boolean) {
        _uiState.update {
            it.copy(shareBattery = state)
        }
    }

    fun approximateLocationChanged(index: Int) {
        _uiState.update {
            it.copy(
                approximateLocationMeters = it.approximateLocationPresetsMeters.getOrNull(index)
                    ?: it.approximateLocationMeters
            )
        }
    }

    fun sharingDurationChanged(index: Int) {
        _uiState.update {
            val time = SharingDurationPreset.presets.getOrNull(index) ?: SharingDurationPreset.hour
            it.copy(
                sharingTimeMillis = time,
                errors = getErrors(LocationSharingContract.isLongTerm(time))
            )
        }
    }

    fun turnOnSpeedSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareSpeed = state)
        }
    }

    fun turnOnElevationSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareElevation = state)
        }
    }

    fun turnOnNetworkSharing(state: Boolean) {
        _uiState.update {
            it.copy(shareNetwork = state)
        }
    }

    fun toggleNotifyOthers(state: Boolean) {
        _uiState.update {
            it.copy(notifyOthers = state)
        }
    }

    fun onDeviceNameChange(name: String) {
        if (_uiState.value.loadingAction) return
        _uiState.update { it.copy(deviceName = name) }
    }

    private fun getErrors(forLongTerm: Boolean): List<ConditionErrorType> {
        return buildList {
            addAll(
                locationStatusProvider.currentStatus.getPossibleErrors(forLongTerm)
            )
            if(!connectionStatus.status.value.isOnline) {
                add(ConditionErrorType.NETWORK)
            }
        }
    }

}