package app.paralino.core.domain.notifications

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.notifications.processors.GroupMemberAddedNotificationProcessor
import app.paralino.core.domain.notifications.processors.GroupMemberDeletedNotificationProcessor
import app.paralino.core.domain.notifications.processors.INotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationRequestNotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationSharingStartedNotificationsProcessor
import app.paralino.core.domain.notifications.processors.LowBatteryNotificationProcessor
import app.paralino.core.domain.notifications.processors.NotificationPreCheckProcessor
import app.paralino.core.domain.notifications.processors.PlaceActivityNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceCreatedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceDeletedNotificationProcessor
import app.paralino.core.domain.notifications.processors.PlaceUpdatedNotificationProcessor
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.raw.LocationFocusRaw
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.base.RemotePreference
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class PushNotificationHandler constructor(
    private val sharingStartedNotification: LocationSharingStartedNotificationsProcessor,
    private val memberAddedNotification: GroupMemberAddedNotificationProcessor,
    private val memberDeletedNotification: GroupMemberDeletedNotificationProcessor,
    private val placeCreatedNotification: PlaceCreatedNotificationProcessor,
    private val placeDeletedNotification: PlaceDeletedNotificationProcessor,
    private val placeUpdatedNotification: PlaceUpdatedNotificationProcessor,
    private val placeActivityNotification: PlaceActivityNotificationProcessor,
    private val locationRequestNotification: LocationRequestNotificationProcessor,
    private val lowBatteryNotificationProcessor: LowBatteryNotificationProcessor,
    private val notificationPreCheck: NotificationPreCheckProcessor,
    private val placePresenter: IPlaceActivityNotificationPresenter,
    private val groupPresenter: IGroupActivityNotificationPresenter,
    private val docApi: IDocumentsApi,
    private val preRefreshUseCase: GroupPreRefreshUseCase,
    private val personalSpaceRefresh: PersonalSpaceRefreshUseCase,
    private val notificationDelete: PushNotificationDeleteUseCase,
    private val appPrefs: AppPrefs,
) : LimitedExecutionRepository() {

    private val executionKey = "notification_sync"

    suspend fun checkForPushNotifications(): Result<Unit> {
        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag(executionKey).v("Processing, can't sync notifications")
            return Result.failure(e)
        }

        delay(2.seconds)

        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = executionKey,
                    timeMillis = 5.seconds.inWholeMilliseconds
                ),
                remote = { checkForPushNotificationsImpl() }
            )
        } catch (e: Exception) {
            logg.tag(executionKey).w("Error refreshing push notifications - ${e.message}")
            return Result.failure(e)
        } finally {
            stopProcessing(executionKey)
        }

        return Result.success(Unit)
    }

    private suspend fun checkForPushNotificationsImpl() {
        logg.tag("push_notifications").i("Start checking push notifications")

        val deviceQuery = listOfNotNull(appPrefs.deviceId, appPrefs.sessionId)
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { BackendQuery.Equals(key = "deviceId", value = it) }

        val notifications = docApi.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = BackendResourceIDs.collectionPushNotificationID,
            queries = buildList {
                if (deviceQuery != null) {
                    add(
                        BackendQuery.OR(
                            queries = listOf(deviceQuery, BackendQuery.IsNull(key = "deviceId"))
                        )
                    )
                } else {
                    add(BackendQuery.IsNull(key = "deviceId"))
                }
            }
        )
            .also {
                logg.tag("push_notifications").v("Got ${it.size} notifications documents")
            }
            .mapNotNull {
                try {
                    PushNotificationRaw.from(it)
                } catch (e: Exception) {
                    logg.tag("push_notifications").e("Error parsing push notification", e)
                    null
                }
            }

        val locationFocusDocuments = docApi.listAllDocuments(
            databaseType = BackendDatabaseType.DATA,
            collectionId = BackendResourceIDs.collectionLocationFocusID
        )
            .also {
                logg.tag("push_notifications").v("Got ${it.size} location focus documents")
            }
            .mapNotNull {
                try {
                    LocationFocusRaw.fromDocument(it)
                } catch (e: Exception) {
                    logg.tag("push_notifications").e("Error parsing location focus", e)
                    null
                }
            }

        logg.tag("push_notifications")
            .v("Got ${notifications.size} notifications documents and ${locationFocusDocuments.size} location focus documents")

        notifications
            .filter { notificationPreCheck.isPushForThisDevice(it) }
            .also {
                logg.tag("push_notifications")
                    .v("Got ${it.size} notifications documents for this device")
            }
            .let {
                // if push have eventId, only use one of each with same event
                // otherwise use all of them
                val withoutEvent = it.filter { it.eventId.isNullOrBlank() }
                val withEvent = it.filter { !it.eventId.isNullOrBlank() }
                    .distinctBy { it.eventId }
                withoutEvent + withEvent
            }
            .also {
                logg.tag("push_notifications")
                    .v("Processing ${it.size} notifications documents for this device")
            }
            .forEach {
                processNotification(
                    push = it,
                    notify = true,
                    onDelete = { push ->
                        notificationDelete.deletePushNotification(notificationId = push.id)
                    }
                )
            }

        locationFocusDocuments
            .groupBy { it.groupId }
            .map {
                // take latest document for each group
                it.value.maxBy { it.updatedAt }
            }
            .map { it.asValidPushNotification() }
            .also {
                logg.tag("push_notifications")
                    .v("Processing ${it.size} location focus documents")
            }
            .forEach {
                processNotification(push = it, notify = true)
            }

        logg.tag("push_notifications").v("Processing notifications done")
    }

    private suspend fun processNotification(
        push: PushNotificationRaw,
        notify: Boolean,
        onDelete: (suspend (PushNotificationRaw) -> Unit)? = null
    ): IPushNotificationPresenter.Payload? {

        val canProcess = notificationPreCheck.preProcess(
            push = push,
            onDelete = onDelete
        )

        if (!canProcess) {
            return null
        }

        val payload = push.pushType?.let {
            val processor = it.processor()
            val presenter = if (notify) it.presenter() else null
            processor.processAndGetPayload(
                push = push,
                presenter = presenter,
                groupRefreshHook = {
                    if (it.groupId.isPersonalSpace) {
                        personalSpaceRefresh.refreshPersonalSpace(
                            cacheTimeMillis = it.cacheTimeDuration.inWholeMilliseconds
                        )
                    } else {
                        preRefreshUseCase.refreshGroupWithMembers(
                            groupId = it.groupId.id,
                            cacheTimeMillis = it.cacheTimeDuration.inWholeMilliseconds
                        )
                    }
                },
                onDelete = onDelete
            )
        }

        return payload
    }

    private fun PushNotificationType.processor(): INotificationProcessor<*> {
        return when (this) {
            PushNotificationType.LOW_BATTERY_ALERT -> lowBatteryNotificationProcessor

            PushNotificationType.GROUP_PLACE_ALERT -> placeActivityNotification
            PushNotificationType.GROUP_PLACE_CREATED -> placeCreatedNotification
            PushNotificationType.GROUP_PLACE_UPDATED -> placeUpdatedNotification
            PushNotificationType.GROUP_PLACE_DELETED -> placeDeletedNotification

            PushNotificationType.PERSONAL_PLACE_ALERT -> placeActivityNotification
            PushNotificationType.PERSONAL_PLACE_CREATED -> placeCreatedNotification
            PushNotificationType.PERSONAL_PLACE_UPDATED -> placeUpdatedNotification
            PushNotificationType.PERSONAL_PLACE_DELETED -> placeDeletedNotification

            PushNotificationType.GROUP_MEMBER_ADDED -> memberAddedNotification
            PushNotificationType.GROUP_MEMBER_DELETED -> memberDeletedNotification
            PushNotificationType.LOCATION_STARTED -> sharingStartedNotification
            PushNotificationType.LOCATION_REQUEST -> locationRequestNotification
            PushNotificationType.LOCATION_FOCUS -> locationRequestNotification
        }
    }

    private fun PushNotificationType.presenter(): IPushNotificationPresenter? {
        return when (this) {
            PushNotificationType.LOW_BATTERY_ALERT -> groupPresenter

            PushNotificationType.GROUP_PLACE_ALERT -> placePresenter
            PushNotificationType.GROUP_PLACE_CREATED -> groupPresenter
            PushNotificationType.GROUP_PLACE_UPDATED -> null
            PushNotificationType.GROUP_PLACE_DELETED -> null

            PushNotificationType.PERSONAL_PLACE_ALERT -> placePresenter
            PushNotificationType.PERSONAL_PLACE_CREATED -> groupPresenter
            PushNotificationType.PERSONAL_PLACE_UPDATED -> null
            PushNotificationType.PERSONAL_PLACE_DELETED -> null

            PushNotificationType.GROUP_MEMBER_ADDED -> groupPresenter
            PushNotificationType.GROUP_MEMBER_DELETED -> null
            PushNotificationType.LOCATION_STARTED -> groupPresenter
            PushNotificationType.LOCATION_REQUEST -> null
            PushNotificationType.LOCATION_FOCUS -> null
        }
    }
}