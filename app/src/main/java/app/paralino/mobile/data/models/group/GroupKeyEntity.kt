package app.paralino.mobile.data.models.group

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_keys")
data class GroupKeyEntity constructor(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val memberUserId: String,
    val version: Long,
    val encryptedGroupKey: String
)