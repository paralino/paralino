package app.paralino.core.db.logs.debug

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "debugLogsEntity")
data class DebugLogsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timeUTC") val timeUTC: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "priority") val priority: Int,
    @ColumnInfo(name = "tag") val tag: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "info", defaultValue = "NULL") val info: String?,
    @ColumnInfo(name = "host", defaultValue = "NULL") val host: String?
)