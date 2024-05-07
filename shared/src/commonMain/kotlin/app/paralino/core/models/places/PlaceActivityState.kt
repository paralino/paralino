package app.paralino.core.models.places

import app.paralino.core.repo.places.PlaceActivityTriggerType
import kotlinx.coroutines.Job

data class PlaceActivityState(
    val placeId: String,
    val groupId: String,
    val isEnter: Boolean,
    val stateChangeTrigger: PlaceActivityTriggerType,
    val stateChangeTimeMillis: Long,

    val isScheduledStateEnter: Boolean? = null,
    val scheduleTrigger: PlaceActivityTriggerType? = null,

    val isLastNotifyStateEnter: Boolean? = null,
    val notifyTrigger: PlaceActivityTriggerType? = null,
    val notifyTimeMillis: Long? = null,

    val tryNotifyJob: Job? = null
)