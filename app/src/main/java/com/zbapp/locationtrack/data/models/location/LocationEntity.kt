package com.zbapp.locationtrack.data.models.location

import android.location.Location
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zbapp.core.models.user.UserMe
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.util.*

@Parcelize
@Entity(tableName = "locations")
data class LocationEntity constructor(
    @PrimaryKey
    val id: String,
    val userId: String,

    val data: String
): Parcelable {

    companion object {
        fun fromSystem(location: Location, sessionId: String, user: UserMe, batteryLevel: Int? = null): LocationEntity {
            return LocationEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                data = ""
            )
        }
    }
}

