package app.paralino.core.presentation.personal_space

import app.paralino.core.resources.moko.*
import app.paralino.core.provider.localizedString

sealed class UserDeviceOrSessionItemState {
    abstract val device: UserDeviceItemState?
    abstract val session: UserSessionItemState?
    abstract val id: String

    val displayName: String get() = when {
        device?.isCurrentDevice == true -> {
            if (!device?.name.isNullOrBlank()) {
                MR.strings.personalSpace_current_device_withName.localizedString(device?.name ?: "")
            } else {
                MR.strings.personalSpace_current_device.localizedString()
            }
        }
        !device?.name.isNullOrBlank() -> device?.name ?: ""
        session != null -> session?.session?.deviceOrClientName ?: "-"
        else -> MR.strings.group_members_device_name_default.localizedString()
    }

    data class DeviceWithSession(
        override val device: UserDeviceItemState,
        override val session: UserSessionItemState?
    ): UserDeviceOrSessionItemState() {
        override val id: String = device.deviceId
    }

    data class SessionWithoutDevice(
        override val session: UserSessionItemState
    ): UserDeviceOrSessionItemState() {
        override val device: UserDeviceItemState? = null
        override val id: String = session.session.id
    }
}