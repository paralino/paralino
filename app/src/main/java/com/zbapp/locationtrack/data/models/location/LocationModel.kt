package com.zbapp.locationtrack.data.models.location

import android.location.Location
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

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

}
