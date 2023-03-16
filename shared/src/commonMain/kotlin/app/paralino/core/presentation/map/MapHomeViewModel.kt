package app.paralino.core.presentation.map

import app.paralino.core.domain.location.LocationRefreshUseCase
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.*
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.IConfig
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class MapHomeViewModel constructor(
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationRefreshUseCase: LocationRefreshUseCase,
    private val userRepository: UserRepository,
    private val config: IConfig
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MapHomeScreenState>(MapHomeScreenState.Empty())
    val uiState: Flow<MapHomeScreenState> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val groupIdData = MutableStateFlow("")
    private val focusedMarker = MutableStateFlow<FocusedMarkerData?>(null)

    val locationRefreshSubscription = uiState.map { (it as? MapHomeScreenState.Ready)?.groupId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { locationRefreshUseCase.locationRefresh(it).mapToPayload { Unit } }

    val allGroups: StateFlow<Resource<List<UserGroup>>> = groupRepo.getAllGroupsLocally()
        .map { Resource.Ready(it) }
        .conflate()
        .distinctUntilChanged { old, new ->
            old.data.sortedBy { it.id }.toTypedArray()
                .contentDeepEquals(new.data.sortedBy { it.id }.toTypedArray())
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.NotInitialized
        )

    init {

        allGroups.onEach { groupsData ->
            if (groupsData is Resource.Ready) {
                val selectedGroup = groupIdData.value
                if (!groupsData.data.any { it.id == selectedGroup }) {
                    switchGroup(groupsData.data.firstOrNull()?.id ?: "")
                }
            }
        }.launchIn(coroutineScope)

        groupIdData
            .flatMapLatest {
                if (it.isBlank()) {
                    userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                        .map {
                            MapHomeScreenState.Empty(
                                it?.userImage ?: ImageModel.emoji(Emojis.robot)
                            )
                        }
                } else {
                    combine(
                        groupRepo.getGroupWithMembersLocally(it),
                        placesRepo.getPlacesForGroupLocally(it),
                        getSharingContract(),
                        focusedMarker,
                        userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                    ) { group, places, sharingStatus, focusedMarker, userData ->

                        val membersData = group.members.flatMap { member ->
                            if (member.locations.isEmpty()) {
                                listOf(
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = GroupMemberState(
                                            member = member.member,
                                            isCurrentUser = member.member.userId == config.userId
                                        ),
                                        location = null
                                    )
                                )
                            } else {
                                val memberState = GroupMemberState(
                                    member = member.member,
                                    isCurrentUser = member.member.userId == config.userId
                                )
                                member.locations.map { location ->
                                    val canDelete = member.member.userId == config.userId &&
                                            location.locationData.data != null &&
                                            !sharingManager.isSharingActiveForGroup(groupIdData.value)
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = memberState.copy(
                                            actionItems = buildList {
                                                if(canDelete) add(MemberAction.DELETE_LOCATION)
                                            }
                                        ),
                                        location = GroupMemberItemLocationState(
                                            memberLocation = location,
                                            name = location.name,
                                            image = member.member.image,
                                            canEdit = false,
                                            currentDevice = location.deviceId == config.deviceId,
                                            showSpeedAndElevation = true
                                        )
                                    )
                                }
                            }
                        }

                        val memberMarkers = group.members
                            .flatMap { member ->
                                member.locations.filter { it.locationData.data != null }
                                    .map { location ->
                                        val memberLocationState =
                                            membersData.first { it.location?.locationId == location.id }
                                        MapPinData(
                                            location = location,
                                            image = member.member.image,
                                        ).copy(
                                            markerId = memberLocationState.keyId,
                                            focused = focusedMarker?.markerId == memberLocationState.keyId
                                        )
                                    }
                            }

                        val placeMarkers =
                            places.filter { it.data.data?.lat != null && it.data.data?.lng != null }
                                .map { place ->
                                    MapPinData(
                                        groupId = group.id,
                                        state = PlaceListItemData(place)
                                    ).copy(
                                        active = true,
                                        focused = focusedMarker?.markerId == place.id
                                    )
                                }

                        MapHomeScreenState.Ready(
                            groupId = group.id,
                            groupName = group.name,
                            groupImage = group.image,
                            userCount = group.memberCount,
                            inactiveUserCount = group.memberCount - group.members.filter { it.hasLocation }.size,
                            currentUserImage = userData?.userImage
                                ?: ImageModel.emoji(Emojis.robot),
                            members = membersData,
                            markers = memberMarkers + placeMarkers,
                            focusedMarker = focusedMarker,
                            sharingStatus = sharingStatus
                        )
                    }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(coroutineScope)
    }

    fun switchGroup(groupId: String) {
        if (groupId.isBlank()) {
            val newGroupId =
                allGroups.value.data?.firstOrNull { it.id == config.selectedGroupId }?.id
                    ?: allGroups.value.data?.firstOrNull()?.id
                    ?: ""
            groupIdData.update { newGroupId }
            config.selectedGroupId = newGroupId
        } else {
            groupIdData.value = groupId
            config.selectedGroupId = groupId
        }
    }

    fun trackMarker(marker: FocusedMarkerData?) {
        focusedMarker.update { marker }
    }

    private fun getSharingContract(): Flow<LocationSharingContract?> {
        return sharingManager.allContractsFlow.combine(groupIdData) { contracts, group ->
            contracts.data?.firstOrNull { it.groupId == group }?.asValidContract()
        }
    }

    suspend fun getAllGroups(): List<GroupItemState> {
        return groupRepo.getAllGroupsLocally().firstOrNull()?.map { group ->
            val uiType = when {
                !group.isVerified -> GroupItemState.GroupUiType.UNVERIFIED
                sharingManager.isSharingActiveForGroup(group.id) -> GroupItemState.GroupUiType.ACTIVE
                else -> GroupItemState.GroupUiType.INACTIVE
            }
            GroupItemState(group, uiType)
        } ?: emptyList()
    }

    fun clearMyLocation(locationId: String) {
        locationRepo.clearLocation(locationId)
            .catch { _eventChannel.send(MapHomeScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }
}