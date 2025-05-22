package app.paralino.core.db.main.keys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personalHistoryKeysEntity")
data class PersonalHistoryKeyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "encryptedHistoryKey") val encryptedHistoryKey: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String
)