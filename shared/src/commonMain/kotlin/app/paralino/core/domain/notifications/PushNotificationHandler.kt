package app.paralino.core.domain.notifications

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.raw.LocationFocusRaw
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.repo.api.BackendQuery
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.base.RemotePreference
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class PushNotificationHandler constructor(
    private val placePush: PlaceActivityNotificationHandler,
    private val groupMemberActivityPush: GroupActivityNotificationsHandler,
    private val locationRequestPush: LocationRequestNotificationHandler,
    private val placePresenter: IPlaceActivityNotificationPresenter,
    private val groupPresenter: IGroupActivityNotificationPresenter,
    private val docApi: IDocumentsApi,
    private val appBuildConfig: AppBuildConfig,
    private val accessLogSource: AccessLogDataSource,
    private val appPrefs: AppPrefs,
) : LimitedExecutionRepository() {

    private val executionKey = "notification_sync"

    suspend fun tryToHandleLocationPush(): Boolean {
        val executionKey = "${executionKey}_location"
        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag(executionKey).v("Processing, can't fetch notification")
            return false
        }

        try {
            accessLogSource.saveLocationRequestAccessLog()

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

            logg.tag("push_notifications")
                .v("Got ${notifications.size} notifications documents")

            notifications
                .filter { it.isForThisDevice() }
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
                .filter {
                    it.pushType == PushNotificationType.LOCATION_FOCUS || it.pushType == PushNotificationType.LOCATION_REQUEST
                }
                .also {
                    logg.tag("push_notifications")
                        .v("Processing ${it.size} location notifications for this device")

                    if (it.isEmpty()) {
                        return false
                    }
                }
                .forEach {
                    processNotification(
                        push = it,
                        notify = false,
                        onDelete = { push ->
                            deletePushNotification(notificationId = push.id)
                        }
                    )
                }
            return true
        } catch (e: Exception) {
            logg.tag(executionKey).w("Error processing location push notification - ${e.message}")
            return false
        } finally {
            startProcessing(executionKey)
        }
    }

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
            .filter { it.isForThisDevice() }
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
                        deletePushNotification(notificationId = push.id)
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

        if (!push.isForThisDevice()) {
            return null
        }

        logg.tag("push_notifications")
            .v("Process ${push.importance} ${push.type} push notification. Expired (${push.isExpired})")

        if (push.isExpired) {
            logg.tag("push_notifications").v("Push notification expired, ignore")
            onDelete?.invoke(push)
            return null
        }

        if (appBuildConfig.appHost == AppHost.MAIN_APP && accessLogSource.canProcessAlertNotification(notificationId = push.id)) {
            accessLogSource.saveAlertNotificationAccessLog(notificationId = push.id)
        }

        val payload: IPushNotificationPresenter.Payload? = when (push.pushType) {
            PushNotificationType.PLACE_ALERT -> {
                val payload = placePush.processPlaceActivity(push)
                val canProcess = accessLogSource.canProcessAlertNotification(notificationId = push.id)

                if (notify && payload != null && canProcess) {
                    placePresenter.notify(payload)
                }

                if (canProcess) {
                    onDelete?.invoke(push)
                } else {
                    logg.tag("push_notifications").i("Can not process push notification: ${push.type}")
                }

                return payload
            }

            PushNotificationType.GROUP_MEMBER_ADDED -> {
                val payload = groupMemberActivityPush.processMemberAdded(push)
                val canProcess = accessLogSource.canProcessAlertNotification(notificationId = push.id)

                if (notify && payload != null && canProcess) {
                    groupPresenter.notify(payload)
                }

                if (canProcess) {
                    onDelete?.invoke(push)
                } else {
                    logg.tag("push_notifications").i("Can not process push notification: ${push.type}")
                }

                return payload
            }

            PushNotificationType.GROUP_MEMBER_DELETED -> {
                val payload = groupMemberActivityPush.processMemberDeleted(push)
                onDelete?.invoke(push)
                return payload
            }

            PushNotificationType.LOCATION_REQUEST, PushNotificationType.LOCATION_FOCUS -> {
                locationRequestPush.process(push)
                onDelete?.invoke(push)
                null
            }

            PushNotificationType.PLACE_CREATED -> {
                val payload = placePush.processPlaceCreated(push)
                val canProcess = accessLogSource.canProcessAlertNotification(notificationId = push.id)

                if (notify && payload != null && canProcess) {
                    groupPresenter.notify(payload)
                }

                if (canProcess) {
                    onDelete?.invoke(push)
                } else {
                    logg.tag("push_notifications").i("Can not process push notification: ${push.type}")
                }

                return payload
            }

            PushNotificationType.PLACE_UPDATED -> {
                val payload = placePush.processPlaceUpdated(push)
                onDelete?.invoke(push)
                return payload
            }

            PushNotificationType.PLACE_DELETED -> {
                val payload = placePush.processPlaceDeleted(push)
                onDelete?.invoke(push)
                return payload
            }

            PushNotificationType.LOCATION_STARTED -> {
                val payload = groupMemberActivityPush.processLocationSharingStarted(push)
                val canProcess = accessLogSource.canProcessAlertNotification(notificationId = push.id)

                if (notify && payload != null && canProcess) {
                    groupPresenter.notify(payload)
                }

                if (canProcess) {
                    onDelete?.invoke(push)
                } else {
                    logg.tag("push_notifications").i("Can not process push notification: ${push.type}")
                }

                return payload
            }

            null -> null
        }

        return payload
    }

    suspend fun deletePushNotification(notificationId: String) {
        try {
            logg.tag("push_notifications").v("Delete push notification document")
            docApi.deleteDocument(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPushNotificationID,
                documentId = notificationId
            )
            delay(400)
        } catch (e: Exception) {
            logg.tag("push_notifications")
                .w("Error deleting push notification $notificationId - ${e.message}")
        } finally {
            accessLogSource.deleteAlertNotificationAccessLog(notificationId = notificationId)
        }
    }

    private fun PushNotificationRaw.isForThisDevice(): Boolean {
        return this.deviceId.isNullOrBlank() || this.deviceId == appPrefs.deviceId || this.deviceId == appPrefs.sessionId
    }
}