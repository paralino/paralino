package app.paralino.core.models.location

import app.paralino.core.appPrefs
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanConfig

data class UserDeviceModel(
    val id: String,
    val sessionId: String?,
    val deviceType: DeviceType?,
    val ownerUserId: String,
    val deviceId: String,
    val userKeyVersion: Long?,
    val location: MemberLocation?
)

data class UserPersonalSpaceModel(
    val devices: List<UserDeviceModel>,
    val plan: PlanConfig,
) {
    val currentDevice: UserDeviceModel? get() {
        val userId = appPrefs.userId
        val deviceId = appPrefs.deviceId

        if (userId.isBlank() || deviceId.isBlank()) {
            throw IllegalArgumentException("Missing current user/device id from preferences")
        }

        val device = devices.firstOrNull { it.deviceId == deviceId }
        return device
    }
}