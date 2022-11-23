package app.paralino.android.data.models.location

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "locations")
data class MemberLocationEntity constructor(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val memberId: String,
    val userId: String,
    val deviceId: String,

    val deviceData: String?,
    val locationData: String?
) : Parcelable

@Entity
data class MemberMutableDataUpdate constructor(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "locationData")
    val locationData: String? = null,
)