package app.paralino.android.data.models.location

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import app.paralino.core.models.group.GroupSettings

@Entity(tableName = "members")
data class MemberEntity constructor(
    @PrimaryKey
    val id: String,
    val userId: String,
    val groupId: String,
    val groupKeyVersion: Long,
    val memberKeyVersion: Long,
    val verification: String,
    val data: String,

    @Embedded(prefix = "settings_")
    val settings: GroupSettings?
)

data class MemberEntityWithLocations constructor(
    @Embedded
    val member: MemberEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "memberId"
    )
    val locations: List<MemberLocationEntity>
)