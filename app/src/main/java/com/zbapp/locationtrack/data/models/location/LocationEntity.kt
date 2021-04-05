package com.zbapp.locationtrack.data.models.location

import android.location.Location
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zbapp.location.models.user.UserMe
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.util.*

@Parcelize
@Entity(tableName = "locations")
data class LocationEntity constructor(
    @PrimaryKey
    val id: String,

    val lat: Double,
    val lng: Double,
    val accuracy: Float = 0f,

    val sessionId: String,
    val circleId: String,

    val name: String,
    val userId: String,
    val imageBase64: String? = null,

    val timeUTC: LocalDateTime? = null,
    val batteryLevel: Int? = null,
): Parcelable {

    companion object {
        fun fromSystem(location: Location, sessionId: String, user: UserMe, batteryLevel: Int? = null): LocationEntity {
            return LocationEntity(
                id = UUID.randomUUID().toString(),
                lat = location.latitude,
                lng = location.longitude,
                accuracy = location.accuracy,
                sessionId = sessionId,
                circleId = "",
                userId = user.id,
                name = user.name,
                imageBase64 = user.imageBase64,
                timeUTC = location.time.toDateTimeUTC(),
                batteryLevel = batteryLevel
            )
        }
    }
}

