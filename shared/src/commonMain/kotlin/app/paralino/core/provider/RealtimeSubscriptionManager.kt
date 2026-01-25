package app.paralino.core.provider

import app.paralino.core.domain.location.RealtimeEventsMaker
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

class RealtimeSubscriptionManager(
    private val eventsMaker: RealtimeEventsMaker,
    private val scopeProvider: ICoroutineScopeProvider
) {

    private var realtimeSubscriptionJob: Job? = null
    private val status = MutableStateFlow(
        RealtimeSubscriptionStatus(
            isSubscribedToNotifications = false,
            isSubscribedToUiData = false
        )
    )

    fun startNotificationsSubscription() {
        status.update { it.copy(isSubscribedToNotifications = true) }
        reInitSubscription()
    }

    fun cancelNotificationsSubscription() {
        status.update { it.copy(isSubscribedToNotifications = false) }
        reInitSubscription()
    }

    fun startUiDataSubscription() {
        status.update { it.copy(isSubscribedToUiData = true) }
        reInitSubscription()
    }

    fun cancelUiDataSubscription() {
        status.update { it.copy(isSubscribedToUiData = false) }
        reInitSubscription()
    }

    private fun reInitSubscription() {
        val set = when {
            status.value.isSubscribedToUiData && status.value.isSubscribedToNotifications -> eventsMaker.eventSetEverything
            status.value.isSubscribedToUiData -> eventsMaker.eventSetUiOnly
            status.value.isSubscribedToNotifications -> eventsMaker.eventSetNotificationOnly
            else -> null
        }

        realtimeSubscriptionJob?.cancel()
        if (set != null) {
            realtimeSubscriptionJob = eventsMaker.listenForEventsWhileActive(eventSet = set)
                .mapToPayload { Unit }
                .launchIn(scopeProvider.userSessionIOScope)
        } else {
            realtimeSubscriptionJob = null
        }
    }

    private data class RealtimeSubscriptionStatus(
        val isSubscribedToNotifications: Boolean,
        val isSubscribedToUiData: Boolean
    )
}