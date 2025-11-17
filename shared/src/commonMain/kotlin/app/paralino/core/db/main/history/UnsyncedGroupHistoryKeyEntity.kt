package app.paralino.core.db.main.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unsyncedGroupHistoryKeyEntity")
data class UnsyncedGroupHistoryKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "memberUserId") val memberUserId: String,
    @ColumnInfo(name = "memberId") val memberId: String,
    @ColumnInfo(name = "memberVerification") val memberVerification: String,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String,
    @ColumnInfo(name = "encryptedHistoryKey") val encryptedHistoryKey: String,
    @ColumnInfo(name = "isSyncedOnce") val isSyncedOnce: Boolean
) {
}