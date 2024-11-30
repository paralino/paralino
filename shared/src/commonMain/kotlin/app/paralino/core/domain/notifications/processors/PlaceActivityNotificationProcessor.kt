package app.paralino.core.domain.notifications.processors

import app.paralino.core.MR
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.domain.notifications.IPushNotificationPresenter
import app.paralino.core.models.data
import app.paralino.core.models.raw.PushNotificationRaw
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.base.AccessLogDataSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesLocalDataSource
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.tryParseFromJson
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

class PlaceActivityNotificationProcessor(
    private val groupRepo: UserGroupLocalDataSource,
    private val placeRepo: PlacesLocalDataSource,
    private val valueFormatter: IValueFormatter,
    private val cryptoCore: CryptoCore,
    private val accessLogSource: AccessLogDataSource,
) : BaseNotificationProcessor(accessLogSource) {

    override suspend fun constructPushPayload(
        push: PushNotificationRaw,
        groupRefreshHook: (suspend (GroupRefreshHookPayload) -> Unit)?
    ): IPushNotificationPresenter.Payload? {

        val payload = push.content?.tryParseFromJson<PlaceActivityNotificationPayload>()
            ?: return null

        var placeName: String? = null
        var userName: String? = null
        var eventSeed: String? = null

        try {
            val place = placeRepo.getPlaceWithoutSettingsOnce(payload.placeId)
            placeName = place?.data?.data?.name
            userName = groupRepo.getMemberOnce(place!!.groupId, payload.memberUserId)?.name
            eventSeed = place.eventSeed
        } catch (_: Exception) {
        }

        val title = if (placeName.isNullOrBlank() || userName.isNullOrBlank()) {
            MR.strings.place_notification_default.localizedString()
        } else {
            val isEnter = when {
                payload.event != null && eventSeed != null -> {
                    when (payload.event) {
                        cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = true) -> true
                        cryptoCore.getPlaceEventKey(seed = eventSeed, isEnter = false) -> false
                        else -> null
                    }
                }

                payload.enter != null -> payload.enter
                else -> null
            }

            when (isEnter) {
                true -> MR.strings.place_notification_enter.localizedString(userName, placeName)
                false -> MR.strings.place_notification_exit.localizedString(userName, placeName)
                else -> MR.strings.place_notification_default.localizedString()
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

        return pushPayload
    }

    @Serializable
    private data class PlaceActivityNotificationPayload constructor(
        val memberUserId: String,
        val locationId: String,
        val placeId: String,

        @Deprecated("Use event instead")
        val enter: Boolean? = null,

        val event: String? = null,
        val triggerAt: String? = null
    )
}