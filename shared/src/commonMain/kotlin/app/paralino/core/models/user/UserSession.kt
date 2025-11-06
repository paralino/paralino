package app.paralino.core.models.user

import app.paralino.core.utils.extensions.isInFuture
import app.paralino.core.utils.extensions.isInPast
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

@Serializable
data class UserSession constructor(
    val id: String,

    val createdAt: String,
    val updatedAt: String,
    val expire: String,
    val provider: String,

    val ip: String,
    val osCode: String,
    val osName: String,
    val osVersion: String,

    val clientType: String,
    val clientCode: String,
    val clientName: String,
    val clientVersion: String,
    val clientEngine: String,
    val clientEngineVersion: String,

    val deviceName: String,
    val deviceBrand: String,
    val deviceModel: String,

    val countryCode: String,
    val countryName: String,
    val current: Boolean
) {
    val isSessionMaybeExpired: Boolean get() = expire.isoWithOffsetToInstant()?.isInPast() ?: false

    val canRenewSessions: Boolean get() = current && (expire.isoWithOffsetToInstant()?.let {
        it.isInFuture() && it.minus(100.days) < nowUTC()
    } ?: true)

    val deviceWithBrandName: String? = listOf(deviceBrand, deviceModel)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .takeIf { it.isNotBlank() }

    val deviceOrClientName: String = listOfNotNull(
        deviceWithBrandName,
        clientName.let {
            if (it == "Android Browser") "Android"
            else it
        }
    )
        .firstOrNull { it.isNotBlank() }
        ?: ""
}
