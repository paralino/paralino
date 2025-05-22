package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GroupLocationHistoryDocumentRaw(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    val dateIdentifier: String,
    val groupId: String,
    val ownerUserId: String,
    val locationId: String?,
    val deviceId: String?,
    val historyData: String,
    val memberKeyVersion: Long,
    val historyKeyVersion: String,
) {
    companion object {
        fun from(map: Map<String, Any>): GroupLocationHistoryDocumentRaw {
            return GroupLocationHistoryDocumentRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).let {
                    try {
                        Instant.parse(it)
                    } catch (e: Exception) {
                        nowUTC()
                    }
                },
                dateIdentifier = map["dateIdentifier"] as String,
                groupId = map["groupId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                locationId = map["locationId"] as? String,
                deviceId = map["deviceId"] as? String,
                historyData = map["historyData"] as String,
                memberKeyVersion = (map["memberKeyVersion"] as Number).toLong(),
                historyKeyVersion = map["historyKeyVersion"] as String,
            )
        }
    }
}