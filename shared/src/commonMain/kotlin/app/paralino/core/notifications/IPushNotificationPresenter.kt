package app.paralino.core.notifications

interface IPushNotificationPresenter {
    fun notify(notificationId: Int, title: String, message: String?, timeMillis: Long)
}

interface IPlaceActivityNotificationPresenter: IPushNotificationPresenter
interface IGroupActivityNotificationPresenter: IPushNotificationPresenter
