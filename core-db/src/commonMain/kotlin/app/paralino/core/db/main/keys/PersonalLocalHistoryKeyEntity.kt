package app.paralino.core.db.main.keys

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.PrimaryKey
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlin.time.Instant

@Entity(tableName = "personalLocalHistoryKeyEntity")
class PersonalLocalHistoryKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "createdAt") val createdAt: String,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long,
    @ColumnInfo(name = "historyKeyVersion") val historyKeyVersion: String,
    @ColumnInfo(name = "encryptedHistoryKey") val encryptedHistoryKey: String,
    @ColumnInfo(name = "isSynced") val isSynced: Boolean
) {
    @Ignore
    val createdAtInstant: Instant? = createdAt.isoWithOffsetToInstant()

    constructor(
        createdAt: String,
        userKeyVersion: Long,
        historyKeyVersion: String,
        encryptedHistoryKey: String,
        isSynced: Boolean,
    ) : this(
        id = 0,
        createdAt = createdAt,
        userKeyVersion = userKeyVersion,
        historyKeyVersion = historyKeyVersion,
        encryptedHistoryKey = encryptedHistoryKey,
        isSynced = isSynced,
    )
}