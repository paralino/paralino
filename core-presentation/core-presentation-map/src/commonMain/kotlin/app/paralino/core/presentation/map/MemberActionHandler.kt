package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.GroupItemState
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.GroupMemberItemState
import app.paralino.core.presentation.common.LocationRequestItemState
import app.paralino.core.presentation.common.MemberActionType
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.onAction
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class MemberActionHandler(
    private val uiState: StateFlow<MapHomeScreenState>,
    private val clearUserLocation: ClearUserLocationUseCase,
    private val locationRequestUseCase: LocationRequestUseCase,
    private val sharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val coroutineScope: CoroutineScope,
    private val eventChannel: kotlinx.coroutines.channels.Channel<MapHomeScreenEvent?>,
    private val locationRequestDuration: kotlin.time.Duration,
) {

    fun memberAction(
        action: MemberActionType,
        state: GroupMemberItemState,
        updateReadyState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        uiState: StateFlow<MapHomeScreenState>
    ) {
        when (action) {
            MemberActionType.DELETE_MEMBER,
            MemberActionType.BLOCK_USER,
            MemberActionType.UNBLOCK_USER -> {
            }

            MemberActionType.DELETE_LOCATION -> {
                clearMyLocation(state, updateReadyState, uiState)
            }
        }
    }

    private fun clearMyLocation(
        state: GroupMemberItemState,
        updateReadyState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        uiState: StateFlow<MapHomeScreenState>
    ) {
        val stateWithLocation = uiState.value.asReady()?.allMembersOrDevices?.firstOrNull { it.keyId == state.keyId }

        val singleLocation = stateWithLocation?.singleLocation ?: return

        clearUserLocation.clearLocation(
            groupId = state.memberState.groupId.asGroupOrSpaceId(),
            locationId = singleLocation.locationId
        )
            .onStart {
                updateReadyState { readyState ->
                    readyState.copy(
                        allMembersOrDevices = readyState.allMembersOrDevices.map {
                            if (it.keyId == state.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        deleteLocation = it.actions.deleteLocation?.copy(isLoading = true)
                                    )
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
                delay(500)
            }
            .onCompletion {
                updateReadyState { readyState ->
                    readyState.copy(
                        allMembersOrDevices = readyState.allMembersOrDevices.map { memberState ->
                            if (memberState.keyId == state.keyId) {
                                memberState.copy(
                                    actions = memberState.actions.copy(
                                        deleteLocation = memberState.actions.deleteLocation
                                            ?.takeIf { action ->
                                                canDeleteLocation(
                                                    groupId = memberState.memberState.groupId.asGroupOrSpaceId(),
                                                    isCurrentUser = memberState.memberState.userId == appPrefs.userId,
                                                    isCurrentDevice = memberState.singleLocation?.currentDevice == true,
                                                    hasLocation = memberState.singleLocation?.visibility?.isVisible() == true
                                                )
                                            }
                                            ?.copy(isLoading = false)
                                    )
                                )
                            } else {
                                memberState
                            }
                        }
                    )
                }

                if (it != null) {
                    messageDispatcher.showSnackbar(it.errorString())
                }
            }
            .catch { /* errors are handled in onCompletion */ }
            .launchIn(coroutineScope)
    }

    fun requestLocationUpdate(
        state: GroupMemberItemState,
        updateReadyState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        getAllGroupsItemState: () -> List<GroupItemState>
    ) {
        val stateWithLocation = uiState.value.asReady()?.allMembersOrDevices?.firstOrNull { it.keyId == state.keyId }

        val singleLocation = stateWithLocation?.singleLocation ?: return
        val locationRequest = stateWithLocation.actions.locationRequest

        if (locationRequest == null) {
            coroutineScope.launch {
                messageDispatcher.showSnackbar(Throwable("Active device not found").errorString())
            }
            return
        }

        if (!locationRequest.isEnabled) {
            coroutineScope.launch {
                messageDispatcher.showSnackbar(MR.strings.group_member_device_request_disabled.localizedString())
            }
            return
        }

        if (!locationRequest.isPremiumFeatureEnabled) {
            coroutineScope.launch {
                val group = getAllGroupsItemState().firstOrNull { it.id == stateWithLocation.memberState.groupId.asGroupOrSpaceId() }
                if (group != null) {
                    if (group.isCurrentUserOwner) {
                        messageDispatcher.showSnackbarForResult(
                            data = AppSnackbarData(
                                text = MR.strings.plan_limit_locationRequest_groupOwner_message.localizedString(),
                                actionText = MR.strings.button_continue.localizedString(),
                                duration = SnackbarDuration.Long
                            )
                        )?.result?.onAction {
                            eventChannel.trySend(MapHomeScreenEvent.OpenPaywall(type = BillingScreenSourceType.UPGRADE_DEFAULT))
                        }
                    } else {
                        messageDispatcher.showSnackbar(MR.strings.plan_limit_locationRequest_groupMember_message.localizedString())
                    }
                }
            }
            return
        }

        flow {
            locationRequestUseCase.requestMemberDeviceLocation(
                groupId = stateWithLocation.memberState.groupId.asGroupOrSpaceId(),
                memberUserId = stateWithLocation.memberState.userId,
                locationId = singleLocation.locationId
            )
            delay(500)
            emit(Unit)
        }
            .onStart {
                updateReadyState { readyState ->
                    readyState.copy(
                        allMembersOrDevices = readyState.allMembersOrDevices.map {
                            if (it.keyId == stateWithLocation.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        locationRequest = LocationRequestItemState(
                                            isLoading = true,
                                            isWaitingForUser = false,
                                            isPremiumFeatureEnabled = readyState.groupPlanConfig.hasLocationRequests
                                        )
                                    )
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
            }
            .onCompletion { error ->
                val hasDeviceUnreachableError = error?.asRemoteException()?.code == 404
                val hasDeviceSetupWrongError = error?.asRemoteException()?.type.let {
                    it == "device_setup_invalid" || it == "push_message_failed"
                }
                val isDeviceMaybeUnreachable = error?.asRemoteException()?.type.let {
                    it == "device_setup_passive"
                }
                updateReadyState { readyState ->
                    readyState.copy(
                        allMembersOrDevices = readyState.allMembersOrDevices.map {
                            if (it.keyId == stateWithLocation.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        locationRequest = it.actions.locationRequest?.copy(
                                            isLoading = error == null,
                                            isEnabled = true,
                                            isWaitingForUser = error == null,
                                            hasRequestError = error != null,
                                            hasDeviceSetupWrongError = hasDeviceSetupWrongError,
                                            hasDeviceUnreachableError = hasDeviceUnreachableError,
                                            hasDeviceMaybeUnreachableError = isDeviceMaybeUnreachable,
                                            lastLocationRequest = nowUTC(),
                                            isPremiumFeatureEnabled = readyState.groupPlanConfig.hasLocationRequests
                                        )
                                    )
                                )
                            } else {
                                it
                            }
                        }
                    )
                }

                if (error != null) {
                    // only show snackbar for other type of errors
                    // these are displayed on member item on map
                    if (isDeviceMaybeUnreachable) {
                        messageDispatcher.showSnackbar(MR.strings.group_member_device_maybe_unreachable_message.localizedString())
                    } else if (!hasDeviceUnreachableError && !hasDeviceSetupWrongError) {
                        if (error.asRemoteException()?.code == 429) {
                            messageDispatcher.showSnackbar(MR.strings.group_member_device_request_disabled.localizedString())
                        } else {
                            messageDispatcher.showSnackbar(error.errorString())
                        }
                    }
                } else {
                    scheduleDeviceResponseCheck()
                }
            }
            .catch { /* errors are handled in onCompletion */ }
            .launchIn(coroutineScope)
    }

    private fun scheduleDeviceResponseCheck() {
        coroutineScope.launch {
            delay(locationRequestDuration)
            val anyObsoleteRequests = uiState.value.asReady()?.allMembersOrDevices?.any { member ->
                member.actions.locationRequest?.let { request ->
                    val isWaiting = request.isWaitingForUser
                    val isObsolete = request.lastLocationRequest?.plus(locationRequestDuration)
                        ?.let { it < nowUTC() } ?: true
                    isWaiting && isObsolete
                } ?: false
            } ?: false

            if (anyObsoleteRequests) {
                messageDispatcher.showSnackbar(
                    text = MR.strings.group_member_device_not_responding_message.localizedString(),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun canDeleteLocation(
        groupId: app.paralino.core.models.base.GroupOrSpaceId,
        isCurrentUser: Boolean,
        isCurrentDevice: Boolean,
        hasLocation: Boolean
    ): Boolean {
        return isCurrentUser
                && hasLocation
                && (!isCurrentDevice || !sharingManager.isSharingActiveForGroupMaybe(groupId))
    }
} 