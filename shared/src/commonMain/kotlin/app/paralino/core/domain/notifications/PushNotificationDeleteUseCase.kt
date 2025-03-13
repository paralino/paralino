package app.paralino.core.domain.notifications

import app.paralino.core.logg
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.repo.api.IDocumentsApi
import app.paralino.core.repo.base.AccessLogDataSource
import kotlinx.coroutines.delay

class PushNotificationDeleteUseCase(
    private val docApi: IDocumentsApi,
    private val accessLogSource: AccessLogDataSource,
) {
    suspend fun deletePushNotification(notificationId: String) {
        try {
            logg.tag("push_notifications").v("Delete push notification document $notificationId")
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
}