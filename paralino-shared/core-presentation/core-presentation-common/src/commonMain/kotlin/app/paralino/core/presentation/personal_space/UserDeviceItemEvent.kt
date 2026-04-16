package app.paralino.core.presentation.personal_space

import app.paralino.core.models.user.UserSession

sealed interface UserDeviceItemEvent {
    data class onDeactivateDeviceClick(val item: UserDeviceItemState): UserDeviceItemEvent
    data class onDeleteDeviceClick(val item: UserDeviceItemState): UserDeviceItemEvent
    data class onOpenSessionClick(val item: UserSession): UserDeviceItemEvent
}