package app.paralino.core.notifications

import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.models.raw.PushNotificationType
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.delay

class PushNotificationHandler constructor(
    private val placePush: PlaceActivityNotificationHandler,
    private val groupMemberActivityPush: GroupActivityNotificationsHandler,
    private val locationRequestPush: LocationRequestNotificationHandler,
    private val docApi: IDocumentsApi,
) {

    suspend fun checkForPushNotifications() {
        blocking {
            logg.tag("push_notifications").v("Start checking push notifications")
            val notifications = docApi.listAllDocuments(
                databaseType = BackendDatabaseType.DATA,
                collectionId = BackendResourceIDs.collectionPushNotificationID
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
                .v("Processing ${notifications.size} notifications documents")

            notifications.forEach {
                processNotification(it) { push ->
                    try {
                        logg.tag("push_notifications").v("Delete push notification document")
                        docApi.deleteDocument(
                            databaseType = BackendDatabaseType.DATA,
                            collectionId = BackendResourceIDs.collectionPushNotificationID,
                            documentId = push.id
                        )
                        delay(400)
                    } catch (e: Exception) {
                        logg.tag("push_notifications")
                            .e("Error deleting push notification ${push.id}", e)
                    }
                }
            }
        }
    }

    private suspend fun processNotification(
        push: PushNotificationRaw,
        onDelete: suspend (PushNotificationRaw) -> Unit
    ) {

        if (push.deviceId != config.deviceId) {
            logg.tag("push_notifications")
                .v("Push notification ${push.type} (${push.importance}) from ${push.source} does not match current device, ignoring")
            return
        }

        logg.tag("push_notifications")
            .v("Process ${push.importance} ${push.type} push notification from ${push.source}. Expired (${push.isExpired})")

        if (push.isExpired) {
            logg.tag("push_notifications").v("Push notification expired, delete and ignore")
            onDelete(push)
            return
        }

        when (push.pushType) {
            PushNotificationType.PLACE_ALERT -> {
                placePush.processPlaceActivity(push)
                onDelete(push)
            }

            PushNotificationType.GROUP_MEMBER_ADDED -> {
                groupMemberActivityPush.processMemberAdded(push)
                onDelete(push)
            }

            PushNotificationType.GROUP_MEMBER_DELETED -> {
                groupMemberActivityPush.processMemberDeleted(push)
                onDelete(push)
            }

            PushNotificationType.LOCATION_REQUEST, PushNotificationType.LOCATION_FOCUS -> {
                locationRequestPush.process(push)
                onDelete(push)
            }

            PushNotificationType.PLACE_CREATED -> {
                placePush.processPlaceCreated(push)
                onDelete(push)
            }

            PushNotificationType.PLACE_UPDATED -> {
                placePush.processPlaceUpdated(push)
                onDelete(push)
            }

            PushNotificationType.PLACE_DELETED -> {
                placePush.processPlaceDeleted(push)
                onDelete(push)
            }

            null -> {}
        }
    }
}