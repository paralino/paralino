package app.paralino.core.notifications

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlaceActivityNotificationHandler(
    private val groupRepo: GroupRepository,
    private val locationRepo: LocationLocalDataSource,
    private val placeRepo: PlacesRepository,
    private val preRefreshUseCase: GroupPreRefreshUseCase,
    private val scopeProvider: ICoroutineScopeProvider,
    private val presenter: IPlaceActivityNotificationPresenter,
    private val valueFormatter: IValueFormatter
) {
    suspend fun processPlaceActivity(push: PushNotificationRaw) {

        val payload = push.content?.tryParseFromJson<PlaceActivityNotificationPayload>() ?: return

        scopeProvider.userSessionIOScope.launch {

            var placeName: String? = null
            var userName: String? = null

            try {
                val place = placeRepo.getPlaceLocally(payload.placeId).firstOrNull()
                placeName = place?.data?.data?.name
                userName = locationRepo.getMemberOnce(place!!.groupId, payload.memberUserId)?.name
            } catch (_: Exception) {
            }

            val title = if (placeName.isNullOrBlank() || userName.isNullOrBlank()) {
                MR.strings.place_notification_default.localizedString()
            } else {
                if (payload.enter) {
                    MR.strings.place_notification_enter.localizedString(userName, placeName)
                } else {
                    MR.strings.place_notification_exit.localizedString(userName, placeName)
                }
            }

            val notificationId = "place_activity_${payload}_${push.createdAt}".hashCode()
            presenter.notify(
                notificationId = notificationId,
                title = title,
                message = null,
                timeMillis = push.createdAt.toEpochMilliseconds(),
                timeInfo = if (push.isOlderThan(2.minutes)) {
                    valueFormatter.formatSimpleDateTime(
                        timeUTC = push.createdAt.toLocalDateTime(TimeZone.UTC),
                        optionalYear = true,
                        optionalMonth = true
                    )
                } else {
                    null
                }
            )
        }
    }

    suspend fun processPlaceCreated(push: PushNotificationRaw) {
        val payload =
            push.content?.tryParseFromJson<PlaceCreateDeleteNotificationPayload>() ?: return

        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 20.seconds.inWholeMilliseconds
        )

        var placeName: String? = null
        var groupName: String? = null

        try {
            val place = placeRepo.getPlaceLocally(payload.placeId).firstOrNull()
            placeName = place?.nameOrNull
            groupName = groupRepo.getGroupWithoutMembersLocally(place!!.groupId).first().nameOrNull
        } catch (e: Exception) {
        }

        val title = if (placeName.isNullOrBlank() || groupName.isNullOrBlank()) {
            MR.strings.place_created_notification_default.localizedString()
        } else {
            MR.strings.place_created_notification.localizedString(groupName, placeName)
        }

        // TODO use different presenter or channel
        val notificationId = payload.hashCode()
        presenter.notify(
            notificationId = notificationId,
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null
        )
    }

    suspend fun processPlaceDeleted(push: PushNotificationRaw) {
        val payload =
            push.content?.tryParseFromJson<PlaceCreateDeleteNotificationPayload>() ?: return
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 20.seconds.inWholeMilliseconds
        )
    }

    suspend fun processPlaceUpdated(push: PushNotificationRaw) {
        val payload =
            push.content?.tryParseFromJson<PlaceCreateDeleteNotificationPayload>() ?: return
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 20.seconds.inWholeMilliseconds
        )
    }
}

@Serializable
data class PlaceActivityNotificationPayload constructor(
    val memberUserId: String,
    val locationId: String,
    val placeId: String,
    val enter: Boolean
)

@Serializable
data class PlaceCreateDeleteNotificationPayload constructor(
    val groupId: String,
    val placeId: String,
)
