package app.paralino.core.models.location

import app.paralino.core.models.base.DeviceType

data class UserDeviceModel(
    val id: String,
    val sessionId: String?,
    val deviceType: DeviceType?,
    val ownerUserId: String,
    val deviceId: String,
    val userKeyVersion: Long?,
    val location: MemberLocation?
) {
}