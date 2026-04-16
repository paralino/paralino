package app.paralino.core.models.location

import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType

sealed interface LocationHistoryPointState {
    val pointId: String
    val locationId: String?
    val locationTime: String?
    val statusTime: String?
    val formattedSpeed: String?
    val formattedElevation: String?
    val lat: Double?
    val lng: Double?
    val accuracy: Float?

    data class Cluster(
        override val locationId: String?,
        override val pointId: String,
        override val locationTime: String?,
        val points: List<Point>,
        override val lat: Double,
        override val lng: Double,
        override val accuracy: Float,
        val battery: String?,
        val isExpanded: Boolean
    ): LocationHistoryPointState {
        override val statusTime: String? = null
        override val formattedElevation: String? = null
        override val formattedSpeed: String? = null
    }

    data class Point(
        override val locationId: String?,
        override val formattedSpeed: String?,
        override val formattedElevation: String?,
        val dateKey: String,
        val entryId: String,
        val isCachedLocation: Boolean,
        val point: LocationHistoryPointModel,
        override val locationTime: String?,
        override val statusTime: String? = null,
        val deviceStatus: MemberDeviceStatusType?,
        val networkType: NetworkType?,
        val signalStrength: NetworkSignalStrength?
    ): LocationHistoryPointState {
        override val pointId: String = point.properID
        override val lat: Double? = point.loc?.lat
        override val lng: Double? = point.loc?.lng
        override val accuracy: Float? = point.loc?.acc
    }
}