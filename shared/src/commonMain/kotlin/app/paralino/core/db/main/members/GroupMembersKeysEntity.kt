package app.paralino.core.db.main.members

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupMembersKeysEntity")
data class GroupMembersKeysEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "memberKeyVersion") val memberKeyVersion: Long,
    @ColumnInfo(name = "signedMembershipPublicKey") val signedMembershipPublicKey: String,
    @ColumnInfo(name = "signedMemberKxPublicKey") val signedMemberKxPublicKey: String,
    @ColumnInfo(name = "ownerIdentityKey") val ownerIdentityKey: String
)
