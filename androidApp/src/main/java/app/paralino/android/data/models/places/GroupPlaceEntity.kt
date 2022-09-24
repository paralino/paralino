package app.paralino.android.data.models.places

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_places")
data class GroupPlaceEntity constructor(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val ownerUserId: String,
    val ownerPublicKey: String,
    val keyVersion: Long,
    val privatePlace: Boolean,
    val data: String
)
