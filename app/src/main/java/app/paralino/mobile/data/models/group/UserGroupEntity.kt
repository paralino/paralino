package app.paralino.mobile.data.models.group

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
    val ownerPublicKey: String,
    val keyVersion: Long,
    val shouldRotateKeys: Boolean,
    val data: String
) : Parcelable {

}
