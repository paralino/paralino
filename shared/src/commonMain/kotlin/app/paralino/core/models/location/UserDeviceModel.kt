package app.paralino.core.models.location

import app.paralino.core.appPrefs
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PushProvider
import app.paralino.core.models.data
import app.paralino.core.models.group.BatteryNotificationsSettingsData
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserPlans
import kotlinx.serialization.Contextual

data class UserDeviceModel(
    val id: String,
    val sessionId: String?,
    val deviceType: DeviceType?,
    val ownerUserId: String,
    val deviceId: String,
    val userKeyVersion: Long?,
    val location: MemberLocation?,
    @Contextual
    val deviceData: CryptoResource<MemberDeviceData>,
    val batteryAlertSettings: BatteryNotificationsSettingsData?,
    val pushProviderConfig: String?,
    val pushProvider: PushProvider
) {
    val deviceNameOrNull: String? = deviceData.data?.deviceName?.takeIf { it.isNotBlank() }
}

data class UserPersonalSpaceModel(
    val user: UserMe,
    val plans: UserPlans,
    val devices: List<UserDeviceModel>,
    val placesCount: Int
) {
    val plan: PlanConfig = plans.highestValidPlanConfig
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
        return if (plan.personalPlacesLimit >= 0) {
            placesCount > plan.personalPlacesLimit
        } else {
            false
        }
    }

    fun areDevicesOverLimits(): Boolean {
        return activeDeviceCount > plan.personalDevicesLimit
    }

    fun isSpaceOverLimits(): Boolean {
        return arePlacesOverLimits() || areDevicesOverLimits()
    }
}