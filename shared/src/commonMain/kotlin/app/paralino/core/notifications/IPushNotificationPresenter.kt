package app.paralino.core.notifications

interface IPushNotificationPresenter {
    fun notify(
        notificationId: Int,
        title: String,
        message: String?,
        timeMillis: Long,
        timeInfo: String?
    )
}

interface IPlaceActivityNotificationPresenter: IPushNotificationPresenter
interface IGroupActivityNotificationPresenter: IPushNotificationPresenter
