package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.PI
import kotlin.math.cos

@Serializable
data class LocationHistoryModel(
    val history: List<LocationHistoryPointModel>
)

@Serializable
data class LocationHistoryModelForUser(
    val groupId: String,
    val memberUserId: String,
    val history: List<LocationHistoryModelForDevice>
)

@Serializable
data class LocationHistoryModelForDevice(
    val locationId: String?,
    val entryId: String,
    val dateKey: String,
    val historyKeyVersion: String,
    val isCachedLocation: Boolean,
    @Contextual
    val history: CryptoResource<LocationHistoryModel>
)

@Serializable
data class LocationHistoryPointModelForMember(
    val groupId: String,
    val entryId: Long,
    val localHistoryKeyId: Long,
    val history: LocationHistoryPointModel
)

@Serializable
data class LocationHistoryPointModel(
    val id: String = "",
    val loc: LocationHistoryPointLocationModel?,
    val status: LocationHistoryPointStatusModel

) {
    @Transient
    val dayKey = status.startTime.isoToLocalDateTime()?.date?.toString()
    
    @Transient
    val properID: String = "${status.startTime}_${id}"

    fun isLocationUnchanged(location: LocationHistoryPointLocationModel?): Boolean {
        if (location == null && this.loc == null) return true
        if (location == null || this.loc == null) return false

        return this.loc.isWithinRange(location)
                && location.acc == this.loc.acc
                && location.spd == this.loc.spd
                && location.alt == this.loc.alt
    }

    // flat-earth approximation - use for only short distance check
    private fun LocationHistoryPointLocationModel.isWithinRange(
        other: LocationHistoryPointLocationModel,
        thresholdMeters: Double = 10.0
    ): Boolean {
        val avgLat = (this.lat + other.lat) / 2
        val metersPerDegreeLat = 111320.0
        val metersPerDegreeLng = 111320.0 * cos(avgLat * 0.01745329252)

        val deltaLat = (other.lat - this.lat) * metersPerDegreeLat
        val deltaLng = (other.lng - this.lng) * metersPerDegreeLng

        val distanceSquared = deltaLat * deltaLat + deltaLng * deltaLng
        return distanceSquared <= (thresholdMeters * thresholdMeters)
    }

    fun isStatusUnchanged(status: LocationHistoryPointStatusModel): Boolean {
        return status.sts == this.status.sts
                && status.bat == this.status.bat
                && status.chr == this.status.chr
                && status.net == this.status.net
                && status.sig == this.status.sig
    }
}

@Serializable
data class LocationHistoryPointLocationModel(
    val tA: String,
    val tB: String? = null,
    val lat: Double,
    val lng: Double,
    val acc: Float = 0f,
    val spd: Float? = null,
    val alt: Float? = null,
) {
    @Transient
    val startTime: String = tA
    @Transient
    val endTime: String = tB ?: tA
}

@Serializable
data class LocationHistoryPointStatusModel(
    val tA: String,
    val tB: String? = null,
    val sts: String? = null,
    val bat: Int? = null,
    val chr: Int? = null,
    val net: String? = null,
    val sig: Int? = null,
) {
    @Transient
    val startTime: String = tA
    @Transient
    val endTime: String = tB ?: tA

    val isCharging: Boolean get() = chr == 1
}