package app.paralino.core.notifications

import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.logg
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.raw.PushNotificationImportance
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LocationRequestNotificationHandler(
    private val locationPerformanceManager: LocationPerformanceManager,
    private val sharingManager: LocationSharingManager,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val coroutineScopeProvider: ICoroutineScopeProvider
) : LimitedExecution(), IPushNotificationsHandler {

    override fun process(push: PushNotificationRaw) {

        val payload = push.content?.let {
            BaseRemoteModule.formatter.decodeFromString<LocationRequestNotificationPayload>(it)
        }

        if (payload == null) {
            logg.w("Location request payload is NULL")
            return
        }

        val performanceType = push.performanceType ?: return
        val key = push.pushType?.key ?: "location_request"

        coroutineScopeProvider.userSessionIOScope.launch {
            try {
                startProcessing(
                    key = key,
                    delayedThresholdMillis = push.processingDelay.inWholeMilliseconds
                )
                try {
                    updateStatusAndPerformance(
                        groupId = payload.groupId,
                        performanceType = performanceType
                    )
                } finally {
                    stopProcessing(key = key)
                }
            } catch (e: Exception) {
                logg.w("Location $key already in progress", e)
            }
        }
    }

    private suspend fun updateStatusAndPerformance(
        groupId: String,
        performanceType: LocationPerformanceType
    ) {
        if (!sharingManager.isSharingActiveForGroup(id = groupId)) {
            try {
                deviceStatusProcessor.pushDeviceStatus(groupId)
            } catch (e: Exception) {
                logg.w("Error saving location status", e)
            }
            return
        }

        locationPerformanceManager.locationBoostRequested(
            groupId = groupId,
            performanceType = performanceType
        )

        try {
            deviceStatusProcessor.pushDeviceStatus(groupId)
        } catch (e: Exception) {
            logg.w("Error saving location status", e)
        }
    }

    private val PushNotificationRaw.performanceType: LocationPerformanceType?
        get() {
            return when (pushType) {
                PushNotificationType.LOCATION_REQUEST -> {
                    LocationPerformanceType.VERY_HIGH
                }

                PushNotificationType.LOCATION_FOCUS -> {
                    when (pushImportance) {
                        PushNotificationImportance.HIGH -> LocationPerformanceType.VERY_HIGH
                        // TODO check if we should use HIGH by default (this was previous behavior)
                        PushNotificationImportance.MEDIUM -> LocationPerformanceType.HIGH
                        else -> LocationPerformanceType.MEDIUM_HIGH
                    }
                }

                else -> null
            }
        }

    private val PushNotificationRaw.processingDelay: Duration
        get() {
            return when (pushType) {
                PushNotificationType.LOCATION_REQUEST -> {
                    5.seconds
                }

                PushNotificationType.LOCATION_FOCUS -> {
                    when (pushImportance) {
                        PushNotificationImportance.HIGH -> 10.seconds
                        PushNotificationImportance.MEDIUM -> 15.seconds
                        else -> 20.seconds
                    }
                }

                else -> 20.seconds
            }
        }
}

@Serializable
data class LocationRequestNotificationPayload constructor(
    val groupId: String,
    val memberUserId: String
)