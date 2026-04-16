package app.paralino.core.presentation.personal_space

sealed interface UserDeviceAction {

    data class DeleteDevice(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true
    ): UserDeviceAction

    data class DeactivateDevice(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true
    ): UserDeviceAction

    data class DeleteSession(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true
    ): UserDeviceAction

    data object OpenSession: UserDeviceAction
    data object OpenRawData: UserDeviceAction
}