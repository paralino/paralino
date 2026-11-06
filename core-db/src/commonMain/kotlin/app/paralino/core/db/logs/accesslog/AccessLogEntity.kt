package app.paralino.core.db.logs.accesslog

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.PrimaryKey
import app.paralino.core.models.AppHost
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlin.time.Instant


@Entity(tableName = "access_logs")
data class AccessLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "host") val host: String,
    @ColumnInfo(name = "access_id") val accessId: String,
    @ColumnInfo(name = "access_time_utc") val timeUTC: String,
) {
    @Ignore
    val appHostType: AppHost? = AppHost.entries.firstOrNull { it.key == host }

    @Ignore
    val accessTimeUTC: Instant? = timeUTC.isoWithOffsetToInstant()
}