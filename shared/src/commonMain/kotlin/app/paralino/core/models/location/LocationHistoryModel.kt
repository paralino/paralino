package app.paralino.core.models.location

import app.paralino.core.models.CryptoResource
import app.paralino.core.utils.extensions.isoToLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val loc: LocationHistoryPointLocationModel?,
    val status: LocationHistoryPointStatusModel

) {
    @Transient
    val dayKey = status.tB.isoToLocalDateTime()?.date?.toString()

    fun isLocationUnchanged(location: LocationHistoryPointLocationModel?): Boolean {
        if (location == null && this.loc == null) return true
        if (location == null || this.loc == null) return false

        return location.lat == this.loc.lat
                && location.lng == this.loc.lng
                && location.acc == this.loc.acc
                && location.spd == this.loc.spd
                && location.alt == this.loc.alt
    }

    fun isStatusUnchanged(status: LocationHistoryPointStatusModel): Boolean {
        return status.stat == this.status.stat
                && status.bat == this.status.bat
                && status.chrg == this.status.chrg
                && status.net == this.status.net
                && status.sig == this.status.sig
    }
}

@Serializable
data class LocationHistoryPointLocationModel(
    val tA: String,
    val tB: String,
    val lat: Double,
    val lng: Double,
    val acc: Float = 0f,
    val spd: Float? = null,
    val alt: Float? = null,
)

@Serializable
data class LocationHistoryPointStatusModel(
    val tA: String,
    val tB: String,
    val stat: String? = null,
    val bat: Int? = null,
    val chrg: Int? = null,
    val net: String? = null,
    val sig: Int? = null,
)