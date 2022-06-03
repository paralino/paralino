package com.zbapp.core.utils

sealed class ConditionErrorType {
    object GPS: ConditionErrorType()
    object PERMISSION_LOCATION: ConditionErrorType()
    object PERMISSION_LOCATION_PRECISE: ConditionErrorType()
    object PERMISSION_LOCATION_BACKGROUND: ConditionErrorType()
    object NETWORK: ConditionErrorType()
    object EMAIL_VERIFICATION: ConditionErrorType()
}
