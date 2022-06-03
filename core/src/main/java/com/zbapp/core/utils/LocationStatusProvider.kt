package com.zbapp.core.utils

import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.IllegalStateException

interface ILocationStatusProvider {
    var status: MutableStateFlow<LocationConditionStatus>
}

data class LocationConditionStatus constructor(
    val isGpsOn: Boolean,
    val isPermissionAllowed: Boolean,
    val isBackgroundPermissionAllowed: Boolean
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

    companion object {

    }
}

class LocationStatusException(val status: LocationConditionStatus): IllegalStateException(status.toString())