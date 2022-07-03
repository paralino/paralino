package app.paralino.mobile.data.models.location

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "members")
data class MemberEntity constructor(
    @PrimaryKey
    val id: String,
    val userId: String,
    val groupId: String,
    val ownerPublicKey: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val shouldRotateKeys: Boolean,
    val data: String,
    val mutableData: String? = null
) : Parcelable

@Entity
data class MemberMutableDataUpdate(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "mutableData")
    val mutableData: String? = null,
)