package app.paralino.core.utils

sealed interface ConditionErrorType {
    object GPS: ConditionErrorType
    object PermissionNotifications: ConditionErrorType
    sealed class PermissionLocation(): ConditionErrorType {
        object PermissionLocationPrecise: PermissionLocation()
        object PermissionLocationAny: PermissionLocation()
    }
    object PermissionLocationBackground: ConditionErrorType
    object Network: ConditionErrorType
    data class EmailVerification(val email: String): ConditionErrorType
    object GuestAccount: ConditionErrorType
}
