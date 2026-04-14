package app.paralino.core.utils

sealed interface ConditionErrorType {
    object GPS: ConditionErrorType, MandatoryForegroundLocationSharingConditionType
    object PermissionNotifications: ConditionErrorType
    sealed class PermissionLocation(): ConditionErrorType, MandatoryForegroundLocationSharingConditionType {
        object PermissionLocationPrecise: PermissionLocation()
        object PermissionLocationAny: PermissionLocation()
    }
    object PermissionLocationBackground: ConditionErrorType, MandatoryBackgroundLocationSharingConditionType
    object Network: ConditionErrorType, MandatoryForegroundLocationSharingConditionType
    data class EmailVerification(val email: String): ConditionErrorType
    object GuestAccount: ConditionErrorType
    object NewAppUpdate: ConditionErrorType
    object UnrestrictedBackgroundAccess: ConditionErrorType, BackgroundLocationSharingConditionType
}

interface BackgroundLocationSharingConditionType
interface MandatoryBackgroundLocationSharingConditionType: BackgroundLocationSharingConditionType
interface MandatoryForegroundLocationSharingConditionType: MandatoryBackgroundLocationSharingConditionType
