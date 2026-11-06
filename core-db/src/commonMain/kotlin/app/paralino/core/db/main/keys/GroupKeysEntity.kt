package app.paralino.core.db.main.keys

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
