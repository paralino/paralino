package app.paralino.core.presentation.groups.settings

import app.paralino.core.models.base.ImageModel

data class BatteryNotificationScreenState(
    val groupId: String,
    val groupName: String,
    val groupImage: ImageModel,
    val fullLoading: Boolean,
    val isGroupPremium: Boolean,
    val canChangeBatterySettings: Boolean,
    val batteryPercentage: Int,
    val isActiveSwitchLoading: Boolean,
    val isActive: Boolean,
    val settings: List<BatteryNotificationMemberState>
) {
    val batteryPercentageText: String = "$batteryPercentage%"
    val currentStep: Int = batterySteps
        .indexOfFirst { batteryPercentage <= it }
        .takeIf { it >= 0 } ?: 0

    companion object {
        const val defaultBatteryLevel = 10
        val batterySteps = arrayOf(5, 10, 15, 20, 25, 30)

        fun radiusForIndex(index: Int): Int {
            return batterySteps.getOrNull(index) ?: defaultBatteryLevel
        }
    }
}

data class BatteryNotificationMemberState constructor(
    val groupId: String,
    val memberId: String,
    val memberUserId: String,
    val isCurrentUser: Boolean,
    val name: String,
    val image: ImageModel,
    val notify: Boolean,
    val loadingNotify: Boolean,
) {
    val loading get() = loadingNotify
}

sealed interface BatteryNotificationScreenUiEvent {
    data object ToggleBatterySettingForGroup : BatteryNotificationScreenUiEvent
    data class ToggleBatterySettingForUser(val userId: String) : BatteryNotificationScreenUiEvent
    data class ChangeDefaultBatteryLevel(val batteryLevel: Int) : BatteryNotificationScreenUiEvent
}

sealed interface BatteryNotificationScreenEvent {
    data object OpenPaywall : BatteryNotificationScreenEvent
}