package app.paralino.core.presentation.personal_space

import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.presentation.common.GroupMemberItemLocationState
import app.paralino.core.presentation.common.MemberLocationVisibilityType
import app.paralino.core.provider.IValueFormatter

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
    val name: String? = device.deviceNameOrNull
}