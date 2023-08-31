package app.paralino.core.notifications

import app.paralino.core.config
import app.paralino.core.domain.user.RefreshEverythingUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.extensions.blocking
import kotlinx.coroutines.delay

class PushNotificationHandler constructor(
    private val placePush: IPlaceNotificationsHandler,
    private val groupMemberAddPush: IGroupMemberAddedNotificationsHandler,
    private val locationRequestPush: ILocationRequestNotificationsHandler,
    private val docApi: IDocumentsApi,
    private val refreshEverything: RefreshEverythingUseCase
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

    private suspend fun processNotification(push: PushNotificationRaw, onDelete: suspend (PushNotificationRaw) -> Unit) {
        logg.tag("push_notifications")
            .v("Process ${push.importance} ${push.type} push notification from ${push.source}. Expired (${push.isExpired})")

        if (push.isExpired) {
            logg.tag("push_notifications").v("Push notification expired, ignoring")
            onDelete(push)
            return
        }

        when (push.type) {
            PushNotificationRaw.pushTypePlaceAlert -> {
                placePush.process(push)
                onDelete(push)
            }
            PushNotificationRaw.pushTypeGroupMemberAdd -> {
                refreshEverything.scheduleRefreshAllGroupsWithPlaces(
                    key = "group_activity",
                    time = 0
                )
                groupMemberAddPush.process(push)
                onDelete(push)
            }
            PushNotificationRaw.pushTypeLocationRequest -> {
                if (push.deviceId == config.deviceId) {
                    logg.tag("push_notifications").v("Location request notification matches current device")
                    locationRequestPush.process(push)
                    onDelete(push)
                } else {
                    logg.tag("push_notifications").v("Location request notification does not match current device, ignoring")
                }
            }
        }
    }
}