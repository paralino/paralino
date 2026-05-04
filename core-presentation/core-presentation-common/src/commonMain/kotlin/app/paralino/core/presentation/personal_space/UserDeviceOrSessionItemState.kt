package app.paralino.core.presentation.personal_space

sealed interface UserDeviceOrSessionItemState {
    val device: UserDeviceItemState?
    val session: UserSessionItemState?
    val id: String

    data class DeviceWithSession(
        override val device: UserDeviceItemState,
        override val session: UserSessionItemState?
    ): UserDeviceOrSessionItemState {
        override val id: String = device.deviceId
    }

    data class SessionWithoutDevice(
        override val session: UserSessionItemState
    ): UserDeviceOrSessionItemState {
        override val device: UserDeviceItemState? = null
        override val id: String = session.session.id
    }
}