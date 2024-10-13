package app.paralino.core.db.main.members

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupKeysEntity")
data class GroupKeysEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "memberUserId") val memberUserId: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "encryptedGroupKey") val encryptedGroupKey: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String
)
