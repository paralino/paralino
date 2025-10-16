package app.paralino.core.db.main.keys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlin.time.Instant

@Entity(tableName = "groupLocalHistoryKeyEntity")
data class GroupLocalHistoryKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "createdAt") val createdAt: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String,
    @ColumnInfo(name = "encryptedHistoryKey") val encryptedHistoryKey: String,
    @ColumnInfo(name = "isActive") val isActive: Boolean,
) {
    @Ignore
    val createdAtInstant: Instant? = createdAt.isoWithOffsetToInstant()

    constructor(
        groupId: String,
        createdAt: String,
        userKeyVersion: Long,
        historyKeyVersion: String,
        encryptedHistoryKey: String,
        isActive: Boolean,
    ) : this(
        id = 0,
        groupId = groupId,
        createdAt = createdAt,
        userKeyVersion = userKeyVersion,
        historyKeyVersion = historyKeyVersion,
        encryptedHistoryKey = encryptedHistoryKey,
        isActive = isActive,
    )
}