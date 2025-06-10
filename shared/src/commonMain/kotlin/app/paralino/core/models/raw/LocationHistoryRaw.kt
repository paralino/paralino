package app.paralino.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class GroupLocationHistoryReadResponse(
    val entries: List<LocationHistoryReadEntry>,
    val keys: List<LocationHistoryReadKey>,
)

@Serializable
data class LocationHistoryReadEntry(
    val id: String,
    val dateKey: String,
    val deviceId: String?,
    val locationId: String?,
    val keyVersion: String,
    val historyData: String
)

@Serializable
data class LocationHistoryReadKey(
    val memberKeyVersion: Long,
    val keyVersion: String,
    val encryptedKey: String
)