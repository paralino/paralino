package app.paralino.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class GroupLocationHistoryReadResponse(
    val dateKeys: List<String>,
    val entries: List<LocationHistoryReadEntry>,
    val historyKeys: List<LocationHistoryReadKey>,
    val kxKeys: List<PersonalKxReadKey>,
)

@Serializable
data class PersonalLocationHistoryReadResponse(
    val dateKeys: List<String>,
    val entries: List<LocationHistoryReadEntry>,
    val keys: List<LocationHistoryReadKey>,
)

@Serializable
data class LocationHistoryReadEntry(
    val id: String,
    val dateKey: String,
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

@Serializable
data class PersonalKxReadKey(
    val keyId: String,
    val memberKeyVersion: Long,
    val userKeyVersion: Long,
    val encryptedKeys: String
)