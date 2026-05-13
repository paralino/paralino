package app.paralino.core.presentation.personal_space

import app.paralino.core.models.location.UserDeviceModel
import app.paralino.core.presentation.common.GroupMemberItemLocationState

data class UserDeviceItemState(
    val device: UserDeviceModel,
    val location: GroupMemberItemLocationState?,
    val deleteAction: UserDeviceAction.DeleteDevice?,
    val deactivateAction: UserDeviceAction.DeactivateDevice?,
    val isCurrentDevice: Boolean
) {
    val deviceId: String = device.deviceId
    val sessionId: String? = device.sessionId

    val visibleBatteryLevel: Int? = location?.adjustedBatteryLevel
    val keyId: String get() = deviceId
    val name: String? = device.deviceNameOrNull
}