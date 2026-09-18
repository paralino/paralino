package app.paralino.core.db.main.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pendingGroupJoinWelcomeEntity")
data class PendingGroupJoinWelcomeEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "didShowWelcome") val didShowWelcome: Boolean,
    @ColumnInfo(name = "createdAtMillis") val createdAtMillis: Long
)
