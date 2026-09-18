package app.paralino.core.presentation.onboarding

import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.group.PendingGroupJoinWelcomeUseCase
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.Emojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class GroupJoinWelcomeViewModel(
    val groupId: String,
    private val groupRepo: UserGroupLocalDataSource,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val pendingGroupJoinWelcome: PendingGroupJoinWelcomeUseCase,
    private val sharingManager: LocationSharingManager,
    private val placesRepo: PlacesRepository
) : CoreViewModel() {

    private val _uiState = MutableStateFlow(
        GroupJoinWelcomeScreenState(
            isLoading = true,
            groupName = null,
            groupImage = null
        )
    )
    val uiState: StateFlow<GroupJoinWelcomeScreenState> = _uiState

    private val _eventChannel = Channel<GroupJoinWelcomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    init {
        coroutineScope.launch {
            groupPreRefresh.refreshGroupWithMembers(
                groupId = groupId,
                cacheTimeMillis = 1.seconds.inWholeMilliseconds
            )
        }

        combine(
            groupRepo.getGroupWithMembers(groupId),
            sharingManager.sharingContractForGroup(groupId.asGroupOrSpaceId()),
            placesRepo.getPlacesForGroupLocally(groupId)
        ) { group, sharingContract, places ->
            val members = group?.members
                ?.sortedBy { member -> if (member.member.userId == group.ownerUserId) 1 else 0 }
                ?.map { member ->
                    GroupJoinWelcomePreviewChipData(
                        id = member.member.id,
                        name = member.member.name,
                        image = member.member.image ?: ImageModel.emoji(Emojis.lock)
                    )
                }
            val placeItems = places.mapNotNull { place ->
                place.data.data?.let { data ->
                    GroupJoinWelcomePreviewChipData(
                        id = place.id,
                        name = data.name,
                        image = data.image
                    )
                }
            }
            _uiState.update { previous ->
                previous.copy(
                    isLoading = group == null,
                    groupName = group?.name ?: previous.groupName,
                    groupImage = group?.image ?: previous.groupImage,
                    members = members ?: previous.members,
                    places = placeItems,
                    isSharingActive = sharingContract != null
                )
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    fun onEvent(event: GroupJoinWelcomeScreenUiEvent) {
        when (event) {
            GroupJoinWelcomeScreenUiEvent.Confirm -> confirm()
        }
    }

    private fun confirm() {
        when (_uiState.value.step) {
            GroupJoinWelcomeStepContent.WelcomeLanding -> {
                _uiState.update { state ->
                    state.copy(
                        step = if (state.isSharingActive) {
                            GroupJoinWelcomeStepContent.Done
                        } else {
                            GroupJoinWelcomeStepContent.ShareLocation
                        }
                    )
                }
            }

            GroupJoinWelcomeStepContent.ShareLocation -> {
                if (_uiState.value.isSharingActive) {
                    _uiState.update { it.copy(step = GroupJoinWelcomeStepContent.Done) }
                } else {
                    _eventChannel.trySend(GroupJoinWelcomeScreenEvent.OpenSharing)
                    coroutineScope.launch {
                        delay(1000)
                        _uiState.update { current ->
                            if (current.step == GroupJoinWelcomeStepContent.ShareLocation) {
                                current.copy(step = GroupJoinWelcomeStepContent.Done)
                            } else {
                                current
                            }
                        }
                    }
                }
            }

            GroupJoinWelcomeStepContent.Done -> {
                coroutineScope.launch {
                    pendingGroupJoinWelcome.complete(groupId = groupId)
                    _eventChannel.trySend(GroupJoinWelcomeScreenEvent.OpenMap)
                }
            }
        }
    }
}
