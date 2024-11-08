package app.paralino.core.domain.notifications.processors

import app.paralino.core.domain.LimitedExecution
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.base.AccessLogDataSource
import kotlin.time.Duration

abstract class INotificationProcessor(
    private val accessLogSource: AccessLogDataSource
) : LimitedExecution() {

    abstract suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload?

    suspend fun processAndGetPayload(
        push: PushNotificationRaw,
        presenter: IPushNotificationPresenter?,
        groupRefreshHook: suspend (GroupRefreshHookPayload) -> Unit,
        onDelete: (suspend (PushNotificationRaw) -> Unit)?,
    ): IPushNotificationPresenter.Payload? {
        val payload = constructPushPayload(
            push = push,
            groupRefreshHook = groupRefreshHook
        )
        val canProcess = accessLogSource.canProcessAlertNotification(notificationId = push.id)

        if (presenter != null && payload != null && canProcess) {
            presenter.notify(payload)
        }

        if (canProcess) {
            onDelete?.invoke(push)
        } else {
            logg.tag("push_notifications").i("Can not process push notification: ${push.type}")
        }

        return payload
    }

    data class GroupRefreshHookPayload(
        val groupId: String,
        val cacheTimeDuration: Duration
    )
}