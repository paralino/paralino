package app.paralino.core.db.main.keys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalGroupKeyEntity")
data class PersonalGroupKeyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "encryptedUserKeys") val encryptedUserKeys: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "personalGroupKeyVersion") val personalGroupKeyVersion: Long,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "isArchived") val isArchived: Boolean,
)
