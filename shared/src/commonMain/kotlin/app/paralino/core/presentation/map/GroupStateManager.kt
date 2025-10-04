package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.Resource
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupStateManager(
    private val groupRepo: UserGroupLocalDataSource,
    private val personalSpaceRepo: UserPersonalSpaceLocalDataSource,
    private val userRepository: UserRepository,
    private val sharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs,
    private val remoteConfig: RemoteConfigRepository,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val coroutineScope: CoroutineScope,
    private val allGroups: StateFlow<Resource<List<UserGroupWithMetadata>>>,
    private val groupIdData: MutableStateFlow<GroupOrSpaceId>,
    private val eventChannel: Channel<MapHomeScreenEvent?>
) {

    suspend fun switchGroup(viewSource: MapScreenViewSourceType) {
        when (viewSource) {
            is MapScreenViewSourceType.DefaultGroup -> {
                selectGroup(groupId = viewSource.id?.id)
            }

            is MapScreenViewSourceType.DefaultGroupOrSpace -> {
                if (viewSource.id?.id.isNullOrBlank()) {
                    if (appPrefs.isPersonalSpaceSelected) {
                        selectPersonalSpace()
                    } else {
                        selectGroup(groupId = appPrefs.selectedGroupId)
                    }
                } else {
                    if (viewSource.id.isPersonalSpace) {
                        selectPersonalSpace()
                    } else {
                        selectGroup(groupId = viewSource.id.id)
                    }
                }
            }

            MapScreenViewSourceType.PersonalSpace -> {
                selectPersonalSpace()
            }
        }
    }

    private suspend fun selectGroup(groupId: String?) {
        val allGroups = allGroups.mapNotNull { it.data }
            .firstOrNull()
            ?: groupRepo.getAllGroupsWithMetadata().firstOrNull()
            ?: emptyList()

        val matchingGroup = allGroups.firstOrNull { it.id == groupId }
            ?: allGroups.firstOrNull { it.id == appPrefs.selectedGroupId }

        val newGroup = matchingGroup
            ?: allGroups.firstOrNull()

        val newGroupId = newGroup?.id ?: ""

        appPrefs.selectedGroupId = newGroupId
        appPrefs.isPersonalSpaceSelected = false
        groupIdData.update { newGroupId.asGroupOrSpaceId() }

        if (newGroup != null && newGroup.isGroupOverLimits(remoteConfig.getConfigOnce())) {
            if (newGroup.group.isCurrentUserOwner) {
                messageDispatcher.showSnackbar(
                    text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever
                )
                delay(2000)
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
            } else {
                messageDispatcher.showSnackbar(
                    text = MR.strings.plan_limit_overLimit_groupMember_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever
                )
            }
        }
    }

    private suspend fun selectPersonalSpace() {
        appPrefs.isPersonalSpaceSelected = true
        groupIdData.update { UserGroup.groupPersonalDevicesId.asGroupOrSpaceId() }
        val personalSpace = personalSpaceRepo.personalSpaceState.value.data
        if (personalSpace?.isSpaceOverLimits() == true) {
            messageDispatcher.showSnackbar(
                text = MR.strings.plan_limit_overLimit_personalSpace_banner_title.localizedString(),
                duration = SnackbarDuration.Forever
            )
            delay(2000)
            eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
        }
    }

    fun tryCreateGroup() {
        coroutineScope.launch {
            val limit = userRepository.userPlans.value.groupsLimit
            val groupCount = getAllGroupsItemState().filter { it.isCurrentUserOwner }.size
            val maxGroupLimit = remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order }?.groupsLimit

            if (maxGroupLimit != null && groupCount >= maxGroupLimit) {
                // This would need to be handled by the main ViewModel to update alertDialog
                eventChannel.trySend(MapHomeScreenEvent.ShowPlanLimitAlert(limit))
            } else if (limit in 0..groupCount) {
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
            } else {
                eventChannel.trySend(MapHomeScreenEvent.OpenGroupCreateScreen)
            }
        }
    }

    fun getGroupPickerState(showPersonalSpace: Boolean): BottomSheetPickerType.GroupPickerBottomSheet {
        val allGroups = getAllGroupsItemState()
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = false,
            showPersonalSpace = showPersonalSpace,
            userPlan = userRepository.userPlans.value.highestValidPlanConfig,
            groupsLimit = userRepository.userPlans.value.groupsLimit,
            personalSpace = personalSpaceRepo.personalSpaceState.value.data?.let { space ->
                GroupItemState(
                    space = space,
                    config = remoteConfig.getCachedConfigOnce(),
                    uiType = when {
                        space.isSpaceOverLimits() -> GroupItemState.GroupUiType.OVER_LIMITS
                        sharingManager.isSharingActiveForGroupMaybe(UserGroup.groupPersonalDevicesId.asGroupOrSpaceId()) -> GroupItemState.GroupUiType.ACTIVE
                        else -> GroupItemState.GroupUiType.INACTIVE
                    },
                    canEdit = true,
                    isSelected = groupIdData.value.isPersonalSpace,
                )
            },
            createdGroups = allGroups.filter { it.isCurrentUserOwner },
            joinedGroups = allGroups.filter { !it.isCurrentUserOwner }
        )
    }

    private fun getAllGroupsItemState(): List<GroupItemState> {
        return allGroups.value.data?.map { group ->
            val uiType = when {
                !group.isVerified -> GroupItemState.GroupUiType.UNVERIFIED
                group.isGroupOverLimits(remoteConfig.getCachedConfigOnce()) -> GroupItemState.GroupUiType.OVER_LIMITS
                sharingManager.isSharingActiveForGroupMaybe(group.id.asGroupOrSpaceId()) -> GroupItemState.GroupUiType.ACTIVE
                else -> GroupItemState.GroupUiType.INACTIVE
            }
            val isSelected = group.id == groupIdData.value.id
            GroupItemState(
                group = group,
                config = remoteConfig.getCachedConfigOnce(),
                uiType = uiType,
                canEdit = true,
                isSelected = isSelected,
                isLarge = isSelected,
            )
        } ?: emptyList()
    }
} 