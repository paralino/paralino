package app.paralino.core.notifications

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.domain.notifications.PushNotificationDeleteUseCase
import app.paralino.core.domain.notifications.processors.GroupMemberAddedNotificationProcessor
import app.paralino.core.domain.notifications.processors.INotificationProcessor
import app.paralino.core.domain.notifications.processors.LocationSharingStartedNotificationsProcessor
import app.paralino.core.domain.notifications.processors.NotificationPreCheckProcessor
import app.paralino.core.domain.notifications.processors.PlaceActivityNotificationProcessor
import app.paralino.core.logg
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.base.AccessLogDataSource
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

class IOSNSEPushNotificationHandler constructor(
    private val notificationPreCheck: NotificationPreCheckProcessor,
    private val docApi: IDocumentsApi,
    private val notificationDelete: PushNotificationDeleteUseCase,
    private val accessLogSource: AccessLogDataSource,
) : LimitedExecutionRepository(), KoinComponent {

    private val executionKey = "notification_sync"

    private val sharingStartedNotification: LocationSharingStartedNotificationsProcessor by lazy { get() }
    private val memberAddedNotification: GroupMemberAddedNotificationProcessor by lazy { get() }
    private val placeCreatedNotification: IOSNSEPlaceCreatedNotificationProcessor by lazy { get() }
    private val placeActivityNotification: PlaceActivityNotificationProcessor by lazy { get() }

    suspend fun retrievePushPresenterPayload(
        notificationId: String,
        force: Boolean
    ): IPushNotificationPresenter.Payload? {
        val executionKey = "${executionKey}_$notificationId"
        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag(executionKey).v("Processing, can't fetch notification")
            if (!force) {
                return null
            }
        }

        try {
            accessLogSource.saveAlertNotificationAccessLog(notificationId = notificationId)
            delay(3.seconds)
            if (!accessLogSource.canProcessAlertNotification(notificationId = notificationId)) {
                logg.tag("push_notifications")
                    .i("Can not process single notification: $notificationId")
                return null
            }

            val notification = docApi.getDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPushNotificationID,
                documentId = notificationId
            )
                .also {
                    logg.tag("push_notifications").v("Got notification document")
                }
                .let {
                    try {
                        PushNotificationRaw.from(it)
                    } catch (e: Exception) {
                        logg.tag("push_notifications").e("Error parsing push notification", e)
                        null
                    }
                }

            return processNotification(
                push = notification!!
            )
        } catch (e: Exception) {
            logg.tag(executionKey).w("Error fetching push notification - ${e.message}")
            return null
        } finally {
            stopProcessing(executionKey)
        }
    }

    private suspend fun processNotification(
        push: PushNotificationRaw
    ): IPushNotificationPresenter.Payload? {

        val canProcess = notificationPreCheck.preProcess(
            push = push,
            onDelete = null
        )

        if (!canProcess) {
            return null
        }

        val payload = push.pushType?.processor()?.processAndGetPayload(
            push = push,
            presenter = null,
            groupRefreshHook = null,
            onDelete = null
        )

        return payload
    }

    suspend fun deletePushNotification(notificationId: String) {
        notificationDelete.deletePushNotification(notificationId = notificationId)
    }

    private fun PushNotificationType.processor(): INotificationProcessor<*>? {
        return when (this) {
            PushNotificationType.PLACE_ALERT -> placeActivityNotification
            PushNotificationType.PLACE_CREATED -> placeCreatedNotification
            PushNotificationType.GROUP_MEMBER_ADDED -> memberAddedNotification
            PushNotificationType.LOCATION_STARTED -> sharingStartedNotification
            PushNotificationType.PLACE_UPDATED -> null
            PushNotificationType.PLACE_DELETED -> null
            PushNotificationType.GROUP_MEMBER_DELETED -> null
            PushNotificationType.LOCATION_REQUEST -> null
            PushNotificationType.LOCATION_FOCUS -> null
        }
    }
}