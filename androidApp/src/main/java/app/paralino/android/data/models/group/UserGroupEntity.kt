package app.paralino.android.data.models.group

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "groups")
data class UserGroupEntity constructor(
    @PrimaryKey
    val id: String,
    val ownerUserId: String,
    val keyVersion: Long,
    val data: String
) : Parcelable {

}
