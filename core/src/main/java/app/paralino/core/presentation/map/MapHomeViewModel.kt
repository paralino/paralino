package app.paralino.core.presentation.map

import app.paralino.core.config
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.dashboard.GroupItemData
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapHomeViewModel(
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    scope: CoroutineScope,
) : CoreViewModel(scope) {

    private val _uiState = MutableStateFlow<Payload<MapHomeScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<MapHomeScreenState>> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val groupIdData = MutableStateFlow("")
    private val focusedMarkerId = MutableStateFlow("")

    val locationRefreshSubscription = uiState.map { it.data }.filterNotNull()
        .flatMapLatest { locationRepo.locationRefresh(it.groupId).mapToPayload { Unit } }


    init {
        groupIdData
            .flatMapLatest {
                if (it.isBlank()) {
                    flowOf(Payload.Loading)
                } else {
                    combine(
                        locationRepo.getLocationsForGroup(it),
                        getSharingContract(),
                        focusedMarkerId
                    ) { group, sharingStatus, focusedMarkerId ->

                        val memberData = group.members.map { member ->
                            GroupMemberItemState(member).copy(
                                actionItem = MemberAction.DELETE_LOCATION.takeIf {
                                    member.userId == config.userId && member.mutableData != null &&
                                            !sharingManager.isSharingActiveForGroup(groupIdData.value)
                                }
                            )
                        }

                        val markers = group.members.filter { it.mutableData != null }
                            .map { member ->
                                MapMarkerState(
                                    groupId = group.id,
                                    focused = focusedMarkerId == member.id,
                                    active = member.isActive,
                                    image = member.image,
                                    memberData = GroupMemberItemState(member),
                                    locationData = member.mutableData!!
                                )
                            }

                        MapHomeScreenState(
                            groupId = group.id,
                            groupName = group.name,
                            groupImage = group.image,
                            userCount = group.memberCount,
                            inactiveUserCount = group.memberCount - markers.size,
                            members = memberData,
                            markers = markers,
                            focusedMarkerId = focusedMarkerId,
                            sharingStatus = sharingStatus
                        )
                    }
                        .mapToPayload { it }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(coroutineScope)
//        switchGroup("")
    }

    fun switchGroup(groupId: String) {
        if (groupId.isBlank()) {
            _uiState.value = Payload.Loading
            coroutineScope.launch {
                groupIdData.value =
                    groupRepo.getAllGroupIds().firstOrNull()?.firstOrNull() ?: ""
            }
        } else groupIdData.value = groupId
    }

    fun trackMarker(markerId: String?) {
        focusedMarkerId.update { markerId ?: "" }
    }


    private fun getSharingContract(): Flow<LocationSharingContract?> {
        return locationRepo.sharingManager.allContractsFlow.combine(groupIdData) { contracts, group ->
            contracts.data?.firstOrNull { it.groupId == group }?.asValidContract()
        }
    }

    suspend fun getAllGroups(): List<GroupItemData> {
        return groupRepo.getAllGroupsLocally().firstOrNull()?.map { GroupItemData(it) }
            ?: emptyList()
    }

    fun clearMyLocation() {
        locationRepo.clearLocation(groupIdData.value)
            .catch { _eventChannel.send(MapHomeScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }

}