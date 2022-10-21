package app.paralino.android.notification

import app.paralino.core.logg
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.toJsonElement
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement

class PushNotificationHandler(
    private val placePush: PlaceNotificationHandler
) {

    fun processFcmMessage(data: Map<String, Any>) {
        val push = try {
            BaseRemoteModule.formatter.decodeFromJsonElement<PushNotificationBody>(data.toJsonElement())
        } catch (e: Exception) {
            logg.tag("push_notifications").e("Error parsing push notification", e)
            return
        }

        logg.tag("push_notifications")
            .v("Got ${push.pushType} push notification from ${push.source}")

        when (push.pushType) {
            PushNotificationBody.pushTypePlaceAlert -> {
                val placePayload =
                    BaseRemoteModule.formatter.decodeFromString<PlaceActivityNotificationPayload>(
                        push.content
                    )
                placePush.process(placePayload)
            }
        }
    }

}

@kotlinx.serialization.Serializable
data class PushNotificationBody(
    val source: String = "api-server",
    val pushType: String,
    val content: String
) {
    companion object {
        const val pushTypePlaceAlert: String = "place_notify"
    }
}