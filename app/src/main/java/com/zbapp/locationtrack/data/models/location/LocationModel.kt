package com.zbapp.locationtrack.data.models.location

import android.location.Location
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zbapp.location.models.location.BaseLocationModel
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Parcelize
@Entity(tableName = "locations")
data class LocationModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val sessionId: String,
    val userId: String,

    val time: LocalDateTime? = null,
    val batteryLevel: Int? = null,
    val imageBase64: String? = null,
): Parcelable {

    constructor(model: BaseLocationModel): this(
        id = model.id,
        lat = model.lat,
        lng = model.lng,
        accuracy = model.accuracy,
        sessionId = model.sessionId,
        userId = model.userId,
        time = model.time?.let {
            try { LocalDateTime.ofInstant(Instant.ofEpochMilli(it),ZoneOffset.UTC) }
            catch (e: Exception) { null }
        },
        batteryLevel = model.batteryLevel,
        imageBase64 = model.imageBase64
    )

    companion object {
        fun fromSystem(location: Location, sessionId: String, userId: String, batteryLevel: Int? = null): LocationModel {
            return LocationModel(
                lat = location.latitude,
                lng = location.longitude,
                accuracy = location.accuracy,
                sessionId = sessionId,
                userId = userId,
                time = LocalDateTime.ofInstant(Instant.ofEpochMilli(location.time),ZoneOffset.UTC),
                batteryLevel = batteryLevel
            )
        }
    }

    fun toBaseModel(): BaseLocationModel {
        return BaseLocationModel(
            id = id,
            lat = lat,
            lng = lng,
            accuracy = accuracy,
            sessionId = sessionId,
            userId = userId,
            time = time?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
            batteryLevel = batteryLevel,
            imageBase64 = imageBase64
        )
    }

}
