package app.paralino.core.db.main.common

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configNodeEntity")
data class RemoteConfigEntity (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "value") val value: String?
)