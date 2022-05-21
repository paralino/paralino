package com.zbapp.core.presentation.share

import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.presentation.CoreViewModel
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.group.GroupRepository
import com.zbapp.core.utils.ILocationStatusProvider
import com.zbapp.core.utils.SharingDurationPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SharingStartViewModel(
    groupId: String?,
    private val groupRepo: GroupRepository,
    private val sharingManager: LocationSharingManager,
    private val locationStatusProvider: ILocationStatusProvider,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<SharingStartScreenState>(SharingStartScreenState())
    val uiState: Flow<SharingStartScreenState> = _uiState

    private val _eventChannel = Channel<SharingStartScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private var selectedPreset: Long = SharingDurationPreset.hour
    private val isLongTermSharing get() = selectedPreset >= SharingDurationPreset.backgroundAccessThreshold

    init {

        groupId?.let { setSelectedGroup(it) }

        locationStatusProvider.status
            .onEach { status ->
                _uiState.update {
                    it.copy(errors = status.getPossibleErrors(isLongTermSharing))
                }
            }
            .launchIn(coroutineScope)
    }

    suspend fun getAllGroups(): List<GroupItemData> {
        return groupRepo.getAllGroupsLocally().firstOrNull()?.map { GroupItemData(it) }
            ?: emptyList()
    }

    fun setSelectedGroup(groupId: String) {
        coroutineScope.launch {
            getAllGroups().firstOrNull { it.id == groupId }?.let { group ->
                _uiState.update { it.copy(groupState = group) }
            }
        }
    }

    fun startSharing() {

        if (locationStatusProvider.status.value.isOK(forAlwaysOnAccess = isLongTermSharing)) {
            proceedWithSharing()
            return
        }

        _uiState.value.groupState ?: let {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.SelectGroupInfo)
            }
            return
        }

        if (locationStatusProvider.status.value.isOK(forAlwaysOnAccess = false) && isLongTermSharing) {
            // only background location permission is missing
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.LocationStatusInsufficient)
            }
        } else {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.LocationStatusInsufficient)
            }
        }
    }

    fun proceedWithSharing() {
        val group = _uiState.value.groupState ?: let {
            coroutineScope.launch {
                _eventChannel.send(SharingStartScreenEvent.SelectGroupInfo)
            }
            return
        }

        coroutineScope.launch {
            _uiState.update { it.copy(loadingAction = true) }
            sharingManager.startSharing(
                LocationSharingContract(groupId = group.id,
                    LocationSharingContract.forDuration(selectedPreset))
            )
            val active = withTimeoutOrNull(2000) {
                sharingManager.isSharingActiveForGroup(group.id)
            } ?: false

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

    fun sharingDurationChanged(progress: Int) {
        selectedPreset =
            SharingDurationPreset.presets.getOrNull(progress) ?: SharingDurationPreset.hour
    }

}