package app.paralino.core.domain.notifications.processors

import app.paralino.core.domain.location.MemberDeviceStatusProcessor
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.raw.PushNotificationImportance
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LocationRequestNotificationProcessor(
    private val locationPerformanceManager: LocationPerformanceManager,
    private val sharingManager: LocationSharingManager,
    private val deviceStatusProcessor: MemberDeviceStatusProcessor,
    private val memberIdUseCase: UserMemberIdUseCase,
    private val coroutineScopeProvider: ICoroutineScopeProvider,
    private val accessLogSource: AccessLogDataSource,
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<LocationRequestNotificationPayload>()

        if (payload == null) {
            logg.w("Location request payload is NULL")
            return null
        }

        val performanceType = push.performanceType ?: return null
        val key = (push.pushType?.key ?: "location_request") + "_${payload.groupId}"

        logg.tag("location_performance")
            .v("Start location performance request $key - (${push.importance} - ${push.pushImportance}) $performanceType")

        coroutineScopeProvider.userSessionIOScope.launch {
            try {
                startProcessing(
                    key = key,
                    delayedThresholdMillis = push.processingDelay.inWholeMilliseconds
                )
                try {
                    updateStatusAndPerformance(
                        groupId = payload.groupId.asGroupOrSpaceId(),
                        performanceType = performanceType
                    )
                } finally {
                    stopProcessing(key = key)
                }
            } catch (e: Exception) {
                logg.w("Location $key already in progress: ${e.message}")
            }
        }

        return null
    }

    private suspend fun updateStatusAndPerformance(
        groupId: GroupOrSpaceId,
        performanceType: LocationPerformanceType
    ) {
        logg.v("Update location status and performance for group $groupId")
        if (!sharingManager.isSharingActiveForGroup(id = groupId)) {
            if (performanceType == LocationPerformanceType.VERY_HIGH) {
                // update not sharing status if someone manually requested device location
                // and only if not previously updated
                // this is to handle the case if location sharing was stopped while device was offline
                // in that case device status is wrong and was not updated
                if (memberIdUseCase.getCurrentLocationForGroup(groupId)?.isEnded == false) {
                    try {
                        deviceStatusProcessor.pushDeviceStatus(groupId = groupId)
                    } catch (e: Exception) {
                        logg.w("Error saving location status", e)
                    }
                }
            }
            return
        }

        locationPerformanceManager.locationBoostRequested(
            performanceType = performanceType,
            groupId = groupId,
            from = "location request notification"
        )

        try {
            deviceStatusProcessor.pushDeviceStatus(groupId = groupId)
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
                        PushNotificationImportance.HIGH,
                        PushNotificationImportance.MEDIUM -> LocationPerformanceType.HIGH
                        // TODO check if we should use HIGH by default (this was previous behavior)
                        PushNotificationImportance.NORMAL -> LocationPerformanceType.MEDIUM_HIGH
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
                        PushNotificationImportance.NORMAL -> 20.seconds
                        else -> 20.seconds
                    }
                }

                else -> 20.seconds
            }
        }

    @Serializable
    data class LocationRequestNotificationPayload constructor(
        val groupId: String,
        val memberUserId: String
    )
}