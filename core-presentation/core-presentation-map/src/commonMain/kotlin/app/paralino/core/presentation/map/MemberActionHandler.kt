package app.paralino.core.presentation.map

import app.paralino.core.resources.moko.*
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.GroupItemState
import app.paralino.core.presentation.common.GroupMemberItemState
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.LocationRequestItemState
import app.paralino.core.presentation.common.MemberActionType
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.onAction
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

class MemberActionHandler(
    private val uiState: StateFlow<MapHomeScreenState>,
    private val clearUserLocation: ClearUserLocationUseCase,
    private val locationRequestUseCase: LocationRequestUseCase,
    private val sharingManager: LocationSharingManager,
    private val appPrefs: AppPrefs,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val coroutineScope: CoroutineScope,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
    private val locationRequestDuration: Duration,
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
                val errorCode = error?.asRemoteException()?.code
                val errorType = error?.asRemoteException()?.type

                val requestError = when {
                    errorType == "device_setup_invalid" -> LocationRequestItemState.RequestError.DeviceSetupInvalid
                    errorType == "device_setup_passive" -> LocationRequestItemState.RequestError.DeviceSetupPassive
                    errorType == "device_not_found" -> LocationRequestItemState.RequestError.DeviceNotFound
                    errorType == "device_setup_missing" -> LocationRequestItemState.RequestError.DeviceSetupMissing
                    errorType == "push_message_failed" -> LocationRequestItemState.RequestError.PushFailed
                    errorCode == 404 -> LocationRequestItemState.RequestError.Generic(apiErrorType = errorType)
                    errorCode == 429 -> LocationRequestItemState.RequestError.TooManyRequests
                    error != null -> LocationRequestItemState.RequestError.Unknown(
                        apiErrorType = errorType,
                        apiErrorCode = errorCode
                    )
                    else -> null
                }

                updateReadyState { readyState ->
                    readyState.copy(
                        allMembersOrDevices = readyState.allMembersOrDevices.map {
                            if (it.keyId == stateWithLocation.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        locationRequest = it.actions.locationRequest?.copy(
                                            // loading as in we are waiting for device to respond
                                            isLoading = error == null,
                                            isEnabled = true,
                                            isWaitingForUser = error == null,
                                            requestError = requestError,
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
                    // only show snackbar for some type of errors
                    // others are displayed on member item on map
                    when (requestError) {
                        LocationRequestItemState.RequestError.DeviceSetupPassive -> {
                            messageDispatcher.showSnackbar(MR.strings.group_member_device_maybe_unreachable_message.localizedString())
                        }

                        LocationRequestItemState.RequestError.DeviceSetupMissing -> {
                            messageDispatcher.showSnackbar(MR.strings.group_member_device_setup_missing_message.localizedString())
                        }

                        LocationRequestItemState.RequestError.PushFailed -> {
                            messageDispatcher.showSnackbar(MR.strings.group_member_device_push_failed_message.localizedString())
                        }

                        LocationRequestItemState.RequestError.TooManyRequests -> {
                            messageDispatcher.showSnackbar(MR.strings.group_member_device_request_disabled.localizedString())
                        }

                        is LocationRequestItemState.RequestError.Unknown -> {
                            messageDispatcher.showSnackbar(error.errorString())
                        }

                        else -> {}
                    }
                } else {
                    scheduleLocationRefreshFallback(
                        keyId = stateWithLocation.keyId,
                        groupId = stateWithLocation.memberState.groupId.asGroupOrSpaceId(),
                        locationId = singleLocation.locationId
                    )
                    scheduleDeviceResponseCheck(updateReadyState)
                }
            }
            .catch { /* errors are handled in onCompletion */ }
            .launchIn(coroutineScope)
    }

    fun applyHighPrioLocationRequests(
        updateReadyState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit
    ) {
        val matchingKeyIds = uiState.value.asReady()?.allMembersOrDevices
            ?.filter { member ->
                member.singleLocation != null
                        && member.actions.locationRequest?.takeIf { it.isEnabled && it.canInteract } != null
            }
            ?.map { it.keyId }

        if (matchingKeyIds.isNullOrEmpty()) return

        updateReadyState { readyState ->
            readyState.copy(
                allMembersOrDevices = readyState.allMembersOrDevices.map { member ->
                    if (matchingKeyIds.any { it == member.keyId }) {
                        member.copy(
                            actions = member.actions.copy(
                                locationRequest = member.actions.locationRequest?.copy(
                                    isLoading = true,
                                    isEnabled = true,
                                    isWaitingForUser = true,
                                    requestError = null,
                                    lastLocationRequest = nowUTC(),
                                    isPremiumFeatureEnabled = readyState.groupPlanConfig.hasLocationRequests
                                )
                            )
                        )
                    } else {
                        member
                    }
                }
            )
        }

        scheduleDeviceResponseCheck(updateReadyState, notifyIfUnresponsive = false)
    }

    private fun scheduleLocationRefreshFallback(
        keyId: String,
        groupId: GroupOrSpaceId,
        locationId: String
    ) {
        coroutineScope.launch {
            delay(locationRequestDuration * 0.75)

            val isStillWaiting = uiState.value.asReady()?.allMembersOrDevices
                ?.firstOrNull { it.keyId == keyId }
                ?.actions?.locationRequest?.isWaitingForUser == true

            if (!isStillWaiting) return@launch

            try {
                logg.tag("location_request").d("No response for location request, fetching location ($locationId) manually")
                locationRequestUseCase.refreshMemberDeviceLocation(
                    groupId = groupId,
                    locationId = locationId
                )
            } catch (e: Exception) {
                logg.tag("location_request").w("Error while fetching location ($locationId) manually", e)
            }
        }
    }

    private fun scheduleDeviceResponseCheck(
        updateReadyState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        notifyIfUnresponsive: Boolean = true
    ) {
        coroutineScope.launch {
            delay(locationRequestDuration)
            val obsoleteKeyIds = uiState.value.asReady()?.allMembersOrDevices
                ?.filter { member ->
                    member.actions.locationRequest?.let { request ->
                        val isWaiting = request.isWaitingForUser
                        val isObsolete = request.lastLocationRequest?.plus(locationRequestDuration)
                            ?.let { it < nowUTC() } ?: true
                        isWaiting && isObsolete
                    } ?: false
                }
                ?.map { it.keyId }

            if (obsoleteKeyIds.isNullOrEmpty()) return@launch

            updateReadyState { readyState ->
                readyState.copy(
                    allMembersOrDevices = readyState.allMembersOrDevices.map { member ->
                        if (obsoleteKeyIds.all { it == member.keyId }) {
                            member.copy(
                                actions = member.actions.copy(
                                    locationRequest = member.actions.locationRequest?.copy(
                                        isLoading = false,
                                        isWaitingForUser = false,
                                        // reset request time so it doesn't go disabled for a second on group focus event
                                        lastLocationRequest = if (notifyIfUnresponsive) {
                                            member.actions.locationRequest?.lastLocationRequest
                                        } else {
                                            null
                                        }
                                    )
                                )
                            )
                        } else {
                            member
                        }
                    }
                )
            }

            if (notifyIfUnresponsive) {
                messageDispatcher.showSnackbar(
                    text = MR.strings.group_member_device_not_responding_message.localizedString(),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    fun canDeleteLocation(
        groupId: GroupOrSpaceId,
        isCurrentUser: Boolean,
        isCurrentDevice: Boolean,
        hasLocation: Boolean
    ): Boolean {
        return isCurrentUser
                && hasLocation
                && (!isCurrentDevice || !sharingManager.isSharingActiveForGroupMaybe(groupId))
    }
} 