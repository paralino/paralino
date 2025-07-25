package app.paralino.core.models.location

import app.paralino.core.appPrefs
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.user.UserMe

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
    val user: UserMe,
    val plan: PlanConfig,
    val devices: List<UserDeviceModel>,
    val placesCount: Int
) {
    val activeDeviceCount: Int = devices.count { it.location != null }
    val currentDevice: UserDeviceModel? get() {
        val userId = appPrefs.userId
        val deviceId = appPrefs.deviceId

        if (userId.isBlank() || deviceId.isBlank()) {
            throw IllegalArgumentException("Missing current user/device id from preferences")
        }

        val device = devices.firstOrNull { it.deviceId == deviceId }
        return device
    }

    fun arePlacesOverLimits(): Boolean {
        return placesCount > plan.placesLimit
    }

    fun areDevicesOverLimits(): Boolean {
        return activeDeviceCount > plan.personalDevicesLimit
    }

    fun isSpaceOverLimits(): Boolean {
        return arePlacesOverLimits() || areDevicesOverLimits()
    }
}