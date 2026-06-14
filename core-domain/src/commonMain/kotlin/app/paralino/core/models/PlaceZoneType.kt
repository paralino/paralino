package app.paralino.core.models

import kotlin.time.Instant

enum class PlaceZoneType {
    PlaceWithAlert,
    PrivacyZone,
}

sealed interface PlaceZoneExpirationStatus {
    val status: String
    val isExpired: Boolean
    data class ActiveAlways(
        val showStatus: Boolean = false,
        override val status: String = "",
    ): PlaceZoneExpirationStatus {
        override val isExpired: Boolean = false
    }

    data class WithExpiration(
        val expirationTime: Instant,
        override val isExpired: Boolean,
        override val status: String
    ): PlaceZoneExpirationStatus
}