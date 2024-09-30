package app.paralino.core.notifications

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.InAppLogger
import app.paralino.core.provider.InAppLoggerLogType
import app.paralino.core.provider.LogPriority
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PlaceActivityNotificationHandler(
    private val groupRepo: UserGroupLocalDataSource,
    private val placeRepo: PlacesRepository,
    private val preRefreshUseCase: GroupPreRefreshUseCase,
    private val presenter: IPlaceActivityNotificationPresenter,
    private val groupPresenter: IGroupActivityNotificationPresenter,
    private val inAppLogger: InAppLogger,
    private val valueFormatter: IValueFormatter
) {
    suspend fun processPlaceActivity(push: PushNotificationRaw) {

        val payload = push.content?.tryParseFromJson<PlaceActivityNotificationPayload>() ?: return

        var placeName: String? = null
        var userName: String? = null

        try {
            val place = placeRepo.getPlaceLocally(payload.placeId).firstOrNull()
            placeName = place?.data?.data?.name
            userName = groupRepo.getMemberOnce(place!!.groupId, payload.memberUserId)?.name
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

        val triggerTime = payload.triggerAt?.isoWithOffsetToInstant() ?: push.createdAt
        val notificationId = "place_activity_${payload}_${triggerTime}".hashCode()
        val timeInfo = if (triggerTime.isOlderThan(1.minutes)) {
            valueFormatter.formatSimpleDateTime(
                timeUTC = triggerTime.toLocalDateTime(TimeZone.UTC),
                optionalYear = true,
                optionalMonth = false,
                showTodayInfo = false
            )
        } else {
            null
        }
        presenter.notify(
            notificationId = notificationId,
            title = title,
            message = null,
            timeMillis = triggerTime.toEpochMilliseconds(),
            timeInfo = timeInfo
        )

        inAppLogger.log(
            type = InAppLoggerLogType.NOTIFICATIONS,
            priority = LogPriority.INFO,
            tag = "place_notification",
            title = title,
            info = timeInfo
        )
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
            groupName = groupRepo.getGroupWithoutMembers(place!!.groupId).first().nameOrNull
        } catch (e: Exception) {
        }

        val title = if (placeName.isNullOrBlank() || groupName.isNullOrBlank()) {
            MR.strings.place_created_notification_default.localizedString()
        } else {
            MR.strings.place_created_notification.localizedString(groupName, placeName)
        }

        val notificationId = payload.hashCode()
        groupPresenter.notify(
            notificationId = notificationId,
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null
        )

        inAppLogger.log(
            type = InAppLoggerLogType.NOTIFICATIONS,
            priority = LogPriority.INFO,
            tag = "place_notification",
            title = title,
            info = null
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
    val enter: Boolean,
    val triggerAt: String? = null
)

@Serializable
data class PlaceCreateDeleteNotificationPayload constructor(
    val groupId: String,
    val placeId: String,
)
