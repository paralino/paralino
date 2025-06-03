package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PersonalLocationHistoryDocumentRaw(
    @SerialName("\$id")
    val id: String,
    @SerialName("\$createdAt")
    val createdAt: Instant,
    val dateIdentifier: String,
    val ownerUserId: String,
    val locationId: String?,
    val deviceId: String?,
    val historyData: String,
    val historyKeyVersion: String
) {
    companion object {
        fun from(map: Map<String, Any>): PersonalLocationHistoryDocumentRaw {
            return PersonalLocationHistoryDocumentRaw(
                id = map["\$id"] as String,
                createdAt = (map["\$createdAt"] as String).isoWithOffsetToInstant() ?: nowUTC(),
                dateIdentifier = map["dateIdentifier"] as String,
                ownerUserId = map["ownerUserId"] as String,
                locationId = map["locationId"] as? String,
                deviceId = map["deviceId"] as? String,
                historyData = map["historyData"] as String,
                historyKeyVersion = map["historyKeyVersion"] as String
            )
        }
    }
}