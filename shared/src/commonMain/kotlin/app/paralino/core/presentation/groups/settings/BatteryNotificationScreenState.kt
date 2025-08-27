package app.paralino.core.presentation.groups.settings

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.ImageModel

data class BatteryNotificationScreenState(
    val groupId: GroupOrSpaceId,
    val groupName: String,
    val groupImage: ImageModel,
    val fullLoading: Boolean,
    val isActiveSwitchLoading: Boolean,
    val isActive: Boolean,
    val settings: List<BatteryNotificationMemberState>
) {
    val isPersonalSpace = groupId.isPersonalSpace
}

data class BatteryNotificationMemberState constructor(
    val groupId: String,
    val alertItemId: String,
    val isCurrentUserOrDevice: Boolean,
    val name: String,
    val image: ImageModel,
    val tag: String?,
    val notify: Boolean,
    val loadingNotify: Boolean,
) {
    val loading get() = loadingNotify
}

sealed interface BatteryNotificationScreenUiEvent {
    data object ToggleBatterySettingForGroup : BatteryNotificationScreenUiEvent
    data class ToggleBatterySettingForItem(val alertItemId: String) : BatteryNotificationScreenUiEvent
}

sealed interface BatteryNotificationScreenEvent {
    data object OpenPaywall : BatteryNotificationScreenEvent
}