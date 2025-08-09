package app.paralino.core.presentation.personal_space

import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.ConditionErrorType

data class PersonalSpaceScreenState(
    val userName: String,
    val userImage: ImageModel?,
    val isLoadingData: Boolean,
    val loadingAction: Boolean,
    val sharingStatus: LocationSharingState?,
    val devicesWithSessions: List<UserDeviceOrSessionItemState>,
    val devicesLimit: Int,
    val shouldShowPlanBanner: Boolean,
    val arePlacesOverLimit: Boolean?,
    val errors: List<ConditionErrorType>,
    val plan: PlanConfig,
    val alertDialog: MainAlertDialog?
) {
    val activeDevices: List<UserDeviceOrSessionItemState> = devicesWithSessions
        .filter { it.device?.location != null }

    val otherDevices: List<UserDeviceOrSessionItemState> = devicesWithSessions
        .filter { it.device?.location == null }

    val isCurrentDeviceActivated: Boolean = activeDevices.any {
        it.device?.isCurrentDevice == true
    }
}

sealed interface UserDeviceOrSessionItemState {
    val device: UserDeviceItemState?
    val session: UserSessionItemState?
    val id: String

    data class DeviceWithSession(
        override val device: UserDeviceItemState,
        override val session: UserSessionItemState?
    ): UserDeviceOrSessionItemState {
        override val id: String = device.deviceId
    }

    data class SessionWithoutDevice(
        override val session: UserSessionItemState
    ): UserDeviceOrSessionItemState {
        override val device: UserDeviceItemState? = null
        override val id: String = session.session.id
    }
}

data class UserSessionItemState(
    val session: UserSession,
    val deleteSessionAction: UserDeviceAction.DeleteSession
)

data class UserDeviceItemState(
    val device: UserDeviceModel,
    val location: GroupMemberItemLocationState?,
    val deleteAction: UserDeviceAction.DeleteDevice?,
    val deactivateAction: UserDeviceAction.DeactivateDevice?,
    val isCurrentDevice: Boolean
) {
    constructor(
        device: UserDeviceModel,
        deleteAction: UserDeviceAction.DeleteDevice?,
        deactivateAction: UserDeviceAction.DeactivateDevice?,
        isCurrentDevice: Boolean,
        formatter: IValueFormatter
    ) : this(
        device = device,
        location = device.location?.let { location ->
            GroupMemberItemLocationState(
                memberLocation = location,
                name = device.deviceNameOrNull ?: location.name,
                image = null,
                canEdit = true,
                showSpeedAndElevation = false,
                visibility = MemberLocationVisibilityType.VISIBLE_ALWAYS,
                currentDevice = isCurrentDevice,
                formatter = formatter
            )
        },
        deleteAction = deleteAction,
        deactivateAction = deactivateAction,
        isCurrentDevice = isCurrentDevice,
    )

    val deviceId: String = device.deviceId
    val sessionId: String? = device.sessionId

    val visibleBatteryLevel: Int? = location?.adjustedBatteryLevel
    val keyId: String get() = deviceId
    val name: String? = device.deviceNameOrNull ?: location?.name
}

sealed interface UserDeviceAction {

    data class DeleteDevice(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true
    ): UserDeviceAction

    data class DeactivateDevice(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true
    ): UserDeviceAction

    data class DeleteSession(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true
    ): UserDeviceAction

    data object OpenSession: UserDeviceAction
    data object OpenRawData: UserDeviceAction
}

sealed interface PersonalSpaceScreenEvent {
    data object OpenPaywall : PersonalSpaceScreenEvent
    data object OpenRawDataScreen : PersonalSpaceScreenEvent
    data object OpenLocation : PersonalSpaceScreenEvent
    data object OpenPlaces : PersonalSpaceScreenEvent
    data object OpenSharingScreen : PersonalSpaceScreenEvent
    data object OpenTimeline : PersonalSpaceScreenEvent
}

sealed interface PersonalSpaceScreenUiEvent {
    data class DialogAction(val action: AlertDialogAction) : PersonalSpaceScreenUiEvent
    data object OpenPaywall : PersonalSpaceScreenUiEvent
    data object OpenLocation : PersonalSpaceScreenUiEvent
    data object OpenPlaces : PersonalSpaceScreenUiEvent
    data object OpenSharingScreen : PersonalSpaceScreenUiEvent
    data object OpenTimeline : PersonalSpaceScreenUiEvent
    data class OnDeviceEvent(val event: UserDeviceItemEvent) : PersonalSpaceScreenUiEvent
}

sealed interface UserDeviceItemEvent {
    data class onDeactivateDeviceClick(val item: UserDeviceItemState): UserDeviceItemEvent
    data class onDeleteDeviceClick(val item: UserDeviceItemState): UserDeviceItemEvent
    data class onOpenSessionClick(val item: UserSession): UserDeviceItemEvent
    data class onOpenRawData(val item: UserDeviceItemState): UserDeviceItemEvent
}
