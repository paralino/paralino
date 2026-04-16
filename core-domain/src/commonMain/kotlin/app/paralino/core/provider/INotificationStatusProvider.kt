package app.paralino.core.provider

import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.toResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

abstract class INotificationStatusProvider {
    protected val mutableStatus = MutableStateFlow<Resource<NotificationConditionStatus>>(Resource.NotInitialized)
    val status: Flow<NotificationConditionStatus> = mutableStatus.mapNotNull { it.data }.distinctUntilChanged()
    val currentStatus get() = mutableStatus.value.data ?: createCurrentStatus()

    open fun refreshNotificationsStatus() {
        mutableStatus.update { createCurrentStatus().toResource() }
    }

    protected abstract fun createCurrentStatus(): NotificationConditionStatus
}

data class NotificationConditionStatus constructor(
    val isNotificationPermissionAllowed: Boolean
) {
    /**
     * Empty companion object so we can add extensions to it in app modules
     */
    companion object { }
}

data class NotificationPermissionResultEvent(val granted: Boolean): ICoreMessageDispatcherEvent