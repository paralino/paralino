package app.paralino.core.notifications

import app.paralino.core.models.raw.PushNotificationRaw

interface IPushNotificationsHandler {
    fun process(push: PushNotificationRaw)
}

interface IPushNotificationPresenter {
    fun notify(notificationId: Int, title: String, message: String?, timeMillis: Long)
}

interface IPlaceActivityNotificationPresenter: IPushNotificationPresenter
interface IGroupMemberAddedNotificationPresenter: IPushNotificationPresenter
