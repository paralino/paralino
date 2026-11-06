package app.paralino.core.db.main.groups

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "pendingGroupJoinWelcomeEntity")
data class PendingGroupJoinWelcomeEntity(
    @PrimaryKey val groupId: String,
    @ColumnInfo(name = "didShowWelcome") val didShowWelcome: Boolean,
    @ColumnInfo(name = "createdAtMillis") val createdAtMillis: Long
)
