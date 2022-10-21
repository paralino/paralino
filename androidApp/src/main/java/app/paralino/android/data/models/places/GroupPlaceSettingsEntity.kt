package app.paralino.android.data.models.places

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place_settings")
data class GroupPlaceSettingsEntity constructor(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val placeId: String,
    val ownerUserId: String,
    val memberId: String,
    val alertData: String
)
