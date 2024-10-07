package app.paralino.core.notifications

import app.paralino.core.MR
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.tryParseFromJson
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
    private val valueFormatter: IValueFormatter
) {
    suspend fun processPlaceActivity(
        push: PushNotificationRaw,
        notify: Boolean
    ): IPushNotificationPresenter.Payload? {

        val payload =
            push.content?.tryParseFromJson<PlaceActivityNotificationPayload>() ?: return null

        var placeName: String? = null
        var userName: String? = null

        try {
            val place = placeRepo.getPlaceLocallyOnce(payload.placeId)
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

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = notificationId,
            title = title,
            message = null,
            timeMillis = triggerTime.toEpochMilliseconds(),
            timeInfo = timeInfo,
            loggerTag = "place_notification"
        )

        if (notify) {
            presenter.notify(pushPayload)
        }

        return pushPayload
    }

    suspend fun processPlaceCreated(
        push: PushNotificationRaw,
        notify: Boolean
    ): IPushNotificationPresenter.Payload? {
        val payload =
            push.content?.tryParseFromJson<PlaceCreateDeleteNotificationPayload>() ?: return null

        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 20.seconds.inWholeMilliseconds
        )

        var placeName: String? = null
        var groupName: String? = null

        try {
            val place = placeRepo.getPlaceLocallyOnce(payload.placeId)
            placeName = place?.nameOrNull
            groupName = groupRepo.getGroupOnce(payload.groupId)?.nameOrNull
        } catch (e: Exception) {
        }

        val title = if (placeName.isNullOrBlank() || groupName.isNullOrBlank()) {
            MR.strings.place_created_notification_default.localizedString()
        } else {
            MR.strings.place_created_notification.localizedString(groupName, placeName)
        }

        val pushPayload = IPushNotificationPresenter.Payload(
            notificationId = payload.hashCode(),
            title = title,
            message = null,
            timeMillis = push.createdAt.toEpochMilliseconds(),
            timeInfo = null,
            loggerTag = "place_notification"
        )

        if (notify) {
            groupPresenter.notify(pushPayload)
        }

        return pushPayload
    }

    suspend fun processPlaceDeleted(
        push: PushNotificationRaw,
        notify: Boolean
    ): IPushNotificationPresenter.Payload? {
        val payload =
            push.content?.tryParseFromJson<PlaceCreateDeleteNotificationPayload>() ?: return null
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 20.seconds.inWholeMilliseconds
        )

        return null
    }

    suspend fun processPlaceUpdated(
        push: PushNotificationRaw,
        notify: Boolean
    ): IPushNotificationPresenter.Payload? {
        val payload =
            push.content?.tryParseFromJson<PlaceCreateDeleteNotificationPayload>() ?: return null
        preRefreshUseCase.refreshGroupWithMembers(
            groupId = payload.groupId,
            cacheTimeMillis = 20.seconds.inWholeMilliseconds
        )

        return null
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
