package app.paralino.core.db.main.keys

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
