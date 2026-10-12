package app.paralino.core.provider

import app.paralino.core.utils.ConditionErrorType

data class ConditionRequestFinishedEvent(val type: ConditionRequestType) : ICoreMessageDispatcherEvent
data class ConditionRequestPromptedEvent(val type: ConditionRequestType) : ICoreMessageDispatcherEvent

enum class ConditionRequestType {
    LOCATION_PERMISSION,
    LOCATION_SERVICE,
    NOTIFICATIONS,
    BACKGROUND_ACCESS
}

val ConditionErrorType.requestType: ConditionRequestType?
    get() = when (this) {
        is ConditionErrorType.PermissionLocation -> ConditionRequestType.LOCATION_PERMISSION
        ConditionErrorType.PermissionLocationBackground -> ConditionRequestType.LOCATION_PERMISSION
        ConditionErrorType.GPS -> ConditionRequestType.LOCATION_SERVICE
        ConditionErrorType.PermissionNotifications -> ConditionRequestType.NOTIFICATIONS
        ConditionErrorType.UnrestrictedBackgroundAccess -> ConditionRequestType.BACKGROUND_ACCESS
        ConditionErrorType.Network,
        is ConditionErrorType.EmailVerification,
        ConditionErrorType.GuestAccount,
        ConditionErrorType.NewAppUpdate -> null
    }
