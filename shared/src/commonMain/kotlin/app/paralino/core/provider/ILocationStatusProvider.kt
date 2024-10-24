package app.paralino.core.provider

import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.toResource
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

abstract class ILocationStatusProvider {
    protected val mutableStatus = MutableStateFlow<Resource<LocationConditionStatus>>(Resource.NotInitialized)
    val status: Flow<LocationConditionStatus> = mutableStatus.mapNotNull { it.data }.distinctUntilChanged()
    val currentStatus get() = mutableStatus.value.data ?: createCurrentStatus()

    open fun refreshLocationStatus() {
        mutableStatus.update { createCurrentStatus().toResource() }
    }

    protected abstract fun createCurrentStatus(): LocationConditionStatus
}

data class LocationConditionStatus constructor(
    val isGpsOn: Boolean,
    val isFinePermissionAllowed: Boolean,
    val isCoarsePermissionAllowed: Boolean,
    val isBackgroundPermissionAllowed: Boolean
) {

    val isAnyLocationPermissionAllowed: Boolean = isCoarsePermissionAllowed || isFinePermissionAllowed

    fun isOK(forAlwaysOnAccess: Boolean = false): Boolean {
        return isGpsOn && isFinePermissionAllowed && (if (forAlwaysOnAccess) isBackgroundPermissionAllowed else true)
    }

    fun getPossibleErrors(forBackground: Boolean = true): MutableList<ConditionErrorType> {
        val list = mutableListOf<ConditionErrorType>()
        if (!isGpsOn) list.add(ConditionErrorType.GPS)

        if (!isCoarsePermissionAllowed) list.add(ConditionErrorType.PermissionLocation.PermissionLocationAny)
        else if (!isFinePermissionAllowed) list.add(ConditionErrorType.PermissionLocation.PermissionLocationPrecise)

        if (!isBackgroundPermissionAllowed && forBackground) list.add(ConditionErrorType.PermissionLocationBackground)
        return list
    }

    /**
     * Empty companion object so we can add extensions to it in app modules
     */
    companion object { }
}

class LocationStatusException(val status: LocationConditionStatus): IllegalStateException(status.toString())