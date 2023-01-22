package app.paralino.android.data.models.location

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "members")
data class MemberEntity constructor(
    @PrimaryKey
    val id: String,
    val userId: String,
    val groupId: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val shouldRotateKeys: Boolean,
    val verification: String,
    val integrity: String?,
    val data: String,
) : Parcelable

data class MemberEntityWithLocations constructor(
    @Embedded
    val member: MemberEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "memberId"
    )
    val locations: List<MemberLocationEntity>
)