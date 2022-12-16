package app.paralino.core.notifications

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class PushNotificationHandler constructor(
    private val placePush: IPlaceNotificationsHandler,
    private val docApi: IDocumentsApi,
    private val scopeProvider: ICoroutineScopeProvider
) {

    fun checkForPushNotifications() {
        logg.tag("push_notifications").v("Start checking push notifications")
        scopeProvider.userSessionIOScope.launch {
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
            notifications.forEach { processNotification(it) }

            notifications.forEach { push ->
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

    private fun processNotification(push: PushNotificationRaw) {
        logg.tag("push_notifications")
            .v("Process ${push.importance} ${push.type} push notification from ${push.source}. Expired (${push.isExpired})")

        if (push.isExpired) {
            logg.tag("push_notifications").v("Push notification expired, ignoring")
            return
        }

        when (push.type) {
            PushNotificationRaw.pushTypePlaceAlert -> {
                val placePayload = push.content?.let {
                    BaseRemoteModule.formatter.decodeFromString<PlaceActivityNotificationPayload>(it)
                }
                if (placePayload != null) {
                    placePush.process(placePayload)
                }
            }
        }
    }

}