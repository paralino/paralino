package app.paralino.core.presentation.share

import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.utils.*
import app.paralino.core.utils.exceptions.TimeoutException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SharingStartViewModel(
    groupId: String?,
    private val groupRepo: GroupRepository,
    private val sharingManager: LocationSharingManager,
    val locationStatusProvider: ILocationStatusProvider,
    private val connectionStatus: IConnectionStatusProvider,
    private val registerDevice: RegisterDeviceForSharingUseCase,
) : CoreViewModel() {

    private val _uiState =
        MutableStateFlow(SharingStartScreenState(groupState = if (groupId.isNullOrBlank()) Payload.Success(
            null) else Payload.Loading))
    val uiState: Flow<SharingStartScreenState> = _uiState

    private val _eventChannel = Channel<SharingStartScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {

        groupId?.let { setSelectedGroup(it) }

        combine(locationStatusProvider.status, connectionStatus.status) { location, connection ->
            _uiState.update {
                it.copy(
                    errors = buildList {
                        addAll(location.getPossibleErrors(it.isLongTerm))
                        if (!connection.isOnline) {
                            add(ConditionErrorType.NETWORK)
                        }
                    }
                )
            }
        }.launchIn(coroutineScope)
    }

    suspend fun getAllGroups(): List<GroupItemState> {
        return groupRepo.getAllGroupsLocally().firstOrNull()?.map { GroupItemState(it) }
            ?: emptyList()
    }

    fun setSelectedGroup(groupId: String) {
        coroutineScope.launch {
            getAllGroups().firstOrNull { it.id == groupId }?.let { group ->
                setSelectedGroup(group)
            } ?: _uiState.update { it.copy(groupState = Payload.Success(null)) }
        }
    }

    fun setSelectedGroup(group: GroupItemState) {
        _uiState.update { it.copy(groupState = Payload.Success(group)) }
    }

    fun startSharing() {

        if (getErrors(forLongTerm = _uiState.value.isLongTerm).isEmpty()) {
            proceedWithSharing()
            return
        }

        _uiState.value.groupState.data ?: let {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.SelectGroupInfo)
            }
            return
        }

        if (getErrors(forLongTerm = false).isEmpty() && _uiState.value.isLongTerm) {
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

    fun proceedWithSharing() {
        val group = _uiState.value.groupState.data ?: let {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.SelectGroupInfo)
            }
            return
        }

        coroutineScope.launch {
            _uiState.update { it.copy(loadingAction = true) }

            try {
                registerDevice.registerDevice(group.id)
            } catch (e: Exception) {
                logg.e("Unable to register device", e)
                _uiState.update { it.copy(loadingAction = false) }
                _eventChannel.send(SharingStartScreenEvent.CanNotRegisterDevice)
                return@launch
            }

            sharingManager.startSharing(
                LocationSharingContract(
                    groupId = group.id,
                    endTimeUTC = LocationSharingContract.forDuration(_uiState.value.sharingTimeMillis),
                    shareBattery = _uiState.value.shareBattery,
                    shareSpeed = _uiState.value.shareSpeed,
                    shareElevation = _uiState.value.shareElevation,
                    approximateLocationInMeters = _uiState.value.approximateLocationMeters.takeIf { _uiState.value.shareApproximateLocation }
                )
            )
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

    private fun getErrors(forLongTerm: Boolean): List<ConditionErrorType> {
        return buildList {
            addAll(
                locationStatusProvider.status.value.getPossibleErrors(forLongTerm)
            )
            if(!connectionStatus.status.value.isOnline) {
                add(ConditionErrorType.NETWORK)
            }
        }
    }

}