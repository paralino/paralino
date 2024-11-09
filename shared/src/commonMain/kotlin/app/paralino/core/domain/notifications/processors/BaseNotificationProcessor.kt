package app.paralino.core.domain.notifications.processors

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.logg
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.repo.base.AccessLogDataSource
import kotlin.time.Duration

abstract class BaseNotificationProcessor(
    accessLogSource: AccessLogDataSource
) : INotificationProcessor<BaseNotificationProcessor.GroupRefreshHookPayload>(accessLogSource) {
    interface IGroupRefreshHookPayload {
        val groupId: String
        val cacheTimeDuration: Duration
    }

    data class GroupRefreshHookPayload(
        override val groupId: String,
        override val cacheTimeDuration: Duration
    ) : IGroupRefreshHookPayload
}

abstract class INotificationProcessor<HOOKPAYLOAD : BaseNotificationProcessor.IGroupRefreshHookPayload>(
    private val accessLogSource: AccessLogDataSource
) : LimitedExecutionRepository() {

    abstract suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (HOOKPAYLOAD) -> Unit)?
    ): IPushNotificationPresenter.Payload?

    suspend fun processAndGetPayload(
        push: PushNotificationRaw,
        presenter: IPushNotificationPresenter?,
        groupRefreshHook: (suspend (HOOKPAYLOAD) -> Unit)?,
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
}