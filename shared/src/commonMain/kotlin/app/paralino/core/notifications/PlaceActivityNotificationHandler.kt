package app.paralino.core.notifications

import app.paralino.core.MR
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

class PlaceActivityNotificationHandler(
    private val locationRepo: LocationRepository,
    private val placeRepo: PlacesRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val presenter: IPlaceActivityNotificationPresenter
) : IPushNotificationsHandler {

    override fun process(push: PushNotificationRaw) {

        val payload = push.content?.let {
            BaseRemoteModule.formatter.decodeFromString<PlaceActivityNotificationPayload>(it)
        } ?: return

        scopeProvider.userSessionIOScope.launch {

            var placeName: String? = null
            var userName: String? = null

            try {
                val place = placeRepo.getPlaceLocally(payload.placeId).firstOrNull()
                placeName = place?.data?.data?.name
                userName = locationRepo.getMember(place!!.groupId, payload.memberUserId)?.name
            } catch (e: Exception) {
            }

            val title = if (placeName.isNullOrBlank() || userName.isNullOrBlank()) {
                MR.strings.place_notification_default.localizedString()
            } else {
                if (payload.enter)
                    MR.strings.place_notification_enter.localizedString(userName, placeName)
                else
                    MR.strings.place_notification_exit.localizedString(userName, placeName)
            }

            val notificationId = payload.hashCode()
            presenter.notify(notificationId, title, message = null, push.createdAt.toEpochMilliseconds())
        }
    }
}

@Serializable
data class PlaceActivityNotificationPayload constructor(
    val memberUserId: String,
    val locationId: String,
    val placeId: String,
    val enter: Boolean
)
