package app.paralino.core.db.main.keys

import androidx.room3.ColumnInfo
import androidx.room3.Entity


@Entity(
    tableName = "pendingKeyRefreshEntity",
    primaryKeys = ["groupId", "keyType"]
)
data class PendingKeyRefreshEntity(
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "keyType") val keyType: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "updatedAtMillis") val updatedAtMillis: Long
)

enum class PendingKeyRefreshType(val key: String) {
    GROUP("group"),
    MEMBER("member"),
    DEVICE("device")
}

enum class PendingKeyRefreshState(val key: String) {
    STARTED("started"),
    DONE("done")
}
