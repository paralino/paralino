package app.paralino.core.utils

sealed class ConditionErrorType {
    object GPS: ConditionErrorType()
    object PERMISSION_NOTIFICATIONS: ConditionErrorType()
    object PERMISSION_LOCATION: ConditionErrorType()
    object PERMISSION_LOCATION_PRECISE: ConditionErrorType()
    object PERMISSION_LOCATION_BACKGROUND: ConditionErrorType()
    object NETWORK: ConditionErrorType()
    data class EMAIL_VERIFICATION(val email: String): ConditionErrorType()
    object GUEST_ACCOUNT: ConditionErrorType()
}
