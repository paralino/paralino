package app.paralino.core.db.main.keys

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupDeviceIntegrityEntity")
data class GroupDeviceIntegrityEntity(
    @PrimaryKey val id: String,

    /**
     * Used to trigger device key rotation if some of group parameters change
     */
    @ColumnInfo(name = "integrity") val integrity: String?,

    /**
     * Used to prevent key rotation and location sharing if same member is re-added to a group
     */
    @ColumnInfo(name = "safetySignature") val safetySignature: String?,
    @ColumnInfo(name = "userKeyVersion") val userKeyVersion: Long?
)
