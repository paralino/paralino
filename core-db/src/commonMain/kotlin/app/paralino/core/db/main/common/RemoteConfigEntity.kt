package app.paralino.core.db.main.common

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "configNodeEntity")
data class RemoteConfigEntity (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "value") val value: String?
)