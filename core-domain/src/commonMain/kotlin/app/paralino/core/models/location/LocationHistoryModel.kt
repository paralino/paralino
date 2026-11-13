package app.paralino.core.models.location

import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.cos

@Serializable
data class LocationHistoryModel(
    val history: List<LocationHistoryPointModel>
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

    fun isLocationUnchanged(
        location: LocationHistoryPointLocationModel?,
        mode: LocationPerformanceSettingsType.History
    ): Boolean {
        if (location == null && this.loc == null) return true
        if (location == null || this.loc == null) return false

        // Adapt distance threshold based on accuracy - worse accuracy = larger threshold
        val adaptiveDistanceThreshold = when {
            location.acc > 100f -> mode.minUpdateDistanceMeters * 3
            location.acc > 50f -> mode.minUpdateDistanceMeters * 2
            location.acc > 20f -> (mode.minUpdateDistanceMeters * 1.5f).toInt()
            else -> mode.minUpdateDistanceMeters  // Good accuracy
        }

        val locationValid = this.loc.isWithinRange(other = location, thresholdMeters = adaptiveDistanceThreshold)
        
        // Adapt accuracy threshold based on absolute accuracy - worse accuracy = larger threshold
        val adaptiveAccuracyThreshold = when {
            location.acc > 100f || this.loc.acc > 100f -> mode.accuracyThreshold * 3
            location.acc > 50f || this.loc.acc > 50f -> mode.accuracyThreshold * 2
            location.acc > 20f || this.loc.acc > 20f -> (mode.accuracyThreshold * 1.5f).toInt()
            else -> mode.accuracyThreshold  // Good accuracy
        }
        
        val accuracyValid = location.acc in this.loc.acc.minus(adaptiveAccuracyThreshold)..this.loc.acc.plus(adaptiveAccuracyThreshold)

        val speedValid = if (location.spd != null && this.loc.spd != null) {
            location.spd in this.loc.spd.minus(mode.speedThresholdMs)..this.loc.spd.plus(mode.speedThresholdMs)
        } else if (location.spd == null && this.loc.spd == null) {
            // if both null, then nothing changed
            true
        } else {
            // here only one of them is null, something changed
            false
        }

        val elevationValid = if (location.alt != null && this.loc.alt != null) {
            location.alt in this.loc.alt.minus(mode.elevationThreshold)..this.loc.alt.plus(mode.elevationThreshold)
        } else if (location.alt == null && this.loc.alt == null) {
            // if both null, then nothing changed
            true
        } else {
            // here only one of them is null, something changed
            false
        }

        return locationValid
                && location.isLikelyInaccurate == this.loc.isLikelyInaccurate
                && accuracyValid
                && speedValid
                && elevationValid
    }

    // flat-earth approximation - use for only short distance check
    private fun LocationHistoryPointLocationModel.isWithinRange(
        other: LocationHistoryPointLocationModel,
        thresholdMeters: Int = 10
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
                && status.tz == this.status.tz
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
    val ina: Boolean? = null,
) {
    @Transient
    val isLikelyInaccurate: Boolean = ina == true
    @Transient
    val startTime: String = tA
    @Transient
    val endTime: String = tB ?: tA
}

@Serializable
data class LocationHistoryPointStatusModel(
    val tA: String,
    val tB: String? = null,
    val tz: String? = null,
    val sts: String? = null,
    val bat: Int? = null,
    val chr: Int? = null,
    val net: String? = null,
    val sig: Int? = null,
    val cnt: Int? = null
) {
    @Transient
    val startTime: String = tA
    @Transient
    val endTime: String = tB ?: tA

    val isCharging: Boolean get() = chr == 1
}