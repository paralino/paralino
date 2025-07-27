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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow

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
    private val eventChannel: kotlinx.coroutines.channels.Channel<MapHomeScreenEvent?>
) {

    suspend fun switchGroup(groupId: GroupOrSpaceId?) {
        if (groupId?.isPersonalSpace == true) {
            selectPersonalSpace()
        } else {
            val allGroups = allGroups.mapNotNull { it.data }
                .firstOrNull()
                ?: groupRepo.getAllGroupsWithMetadata().firstOrNull()
                ?: emptyList()

            val matchingGroup = allGroups.firstOrNull { it.id == groupId?.id }
                ?: allGroups.firstOrNull { it.id == appPrefs.selectedGroupId }

            if (matchingGroup != null || allGroups.isNotEmpty()) {
                val newGroup = matchingGroup
                    ?: allGroups.firstOrNull()

                val newGroupId = newGroup?.id ?: ""

                appPrefs.selectedGroupId = newGroupId
                groupIdData.update { newGroupId.asGroupOrSpaceId() }

                if (newGroup != null && newGroup.isGroupOverLimits(remoteConfig.getConfigOnce())) {
                    if (newGroup.group.isCurrentUserOwner) {
                        messageDispatcher.showSnackbar(
                            text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                            duration = SnackbarDuration.Forever
                        )
                        delay(1000)
                        eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
                    } else {
                        messageDispatcher.showSnackbar(
                            text = MR.strings.plan_limit_overLimit_groupMember_banner_title.localizedString(),
                            duration = SnackbarDuration.Forever
                        )
                    }
                }
            } else {
                selectPersonalSpace()
            }
        }
    }

    private suspend fun selectPersonalSpace() {
        appPrefs.selectedGroupId = UserGroup.groupPersonalDevicesId
        groupIdData.update { UserGroup.groupPersonalDevicesId.asGroupOrSpaceId() }
        val personalSpace = personalSpaceRepo.personalSpaceState.value.data
        if (personalSpace?.isSpaceOverLimits() == true) {
            // TODO change text
            messageDispatcher.showSnackbar(
                text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                duration = SnackbarDuration.Forever
            )
            delay(1000)
            eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
        }
    }

    fun tryCreateGroup() {
        coroutineScope.launch {
            val limit = userRepository.userPlans.value.groupsLimit
            val groupCount = getAllGroupsItemState().filter { it.isCurrentUserOwner }.size
            val maxGroupLimit = remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order }?.groupsLimit
            val shouldShowFreePlanNoticeScreen = userRepository.shouldShowFreePlanNoticeScreen

            if (maxGroupLimit != null && groupCount >= maxGroupLimit) {
                // This would need to be handled by the main ViewModel to update alertDialog
                eventChannel.trySend(MapHomeScreenEvent.ShowPlanLimitAlert(limit))
            } else if (limit in 0..groupCount) {
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
            } else if (shouldShowFreePlanNoticeScreen) {
                eventChannel.trySend(MapHomeScreenEvent.ShowFreePlanInfo)
            } else {
                eventChannel.trySend(MapHomeScreenEvent.AddGroup)
            }
        }
    }

    fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        val allGroups = getAllGroupsItemState()
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = false,
            userPlan = userRepository.userPlans.value.highestValidPlanConfig,
            groupsLimit = userRepository.userPlans.value.groupsLimit,
            personalSpace = personalSpaceRepo.personalSpaceState.value.data?.let {
                GroupItemState(
                    space = it,
                    config = remoteConfig.getCachedConfigOnce(),
                    uiType = GroupItemState.GroupUiType.ACTIVE,
                    canEdit = true,
                    isSelected = false,
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