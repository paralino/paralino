package app.paralino.core.models.group

import kotlinx.serialization.Serializable

@Serializable
data class GroupSettings(
    val id: String,
    val ownerUserId: String,
    val groupId: String,
    val memberIntegrity: String,
    val groupIntegrity: String?,
    val memberIntegrityUserKeyVersion: Long,
    val groupIntegrityUserKeyVersion: Long?,
    val batteryAlertSettings: BatteryNotificationsSettingsData?
)

@Serializable
data class BatteryNotificationsSettingsData constructor(
    val isActive: Boolean = true,
    val settings: List<BatteryNotificationsSettingsUserEntry> = emptyList()
) {
    companion object {
        const val LOW_BATTERY_LEVEL: Int = 15
    }
}

@Serializable
data class BatteryNotificationsSettingsUserEntry constructor(
    val memberUserId: String,
    val deviceId: String? = null,
    val isActive: Boolean
)
