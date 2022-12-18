package app.paralino.core.utils

import kotlinx.coroutines.flow.MutableStateFlow

interface ILocationStatusProvider {
    val status: MutableStateFlow<LocationConditionStatus>
}

data class LocationConditionStatus constructor(
    val isGpsOn: Boolean,
    val isPermissionAllowed: Boolean,
    val isBackgroundPermissionAllowed: Boolean,
    val isNotificationPermissionAllowed: Boolean
) {

    fun isOK(forAlwaysOnAccess: Boolean = false): Boolean {
        return isGpsOn && isPermissionAllowed && (if (forAlwaysOnAccess) isBackgroundPermissionAllowed else true)
    }

    fun getPossibleErrors(forBackground: Boolean = true): MutableList<ConditionErrorType> {
        val list = mutableListOf<ConditionErrorType>()
        if (!isGpsOn) list.add(ConditionErrorType.GPS)
        if (!isPermissionAllowed) list.add(ConditionErrorType.PERMISSION_LOCATION)
        if (!isBackgroundPermissionAllowed && forBackground) list.add(ConditionErrorType.PERMISSION_LOCATION_BACKGROUND)
        return list
    }

    /**
     * Empty companion object so we can add extensions to it in app modules
     */
    companion object { }
}

class LocationStatusException(val status: LocationConditionStatus): IllegalStateException(status.toString())