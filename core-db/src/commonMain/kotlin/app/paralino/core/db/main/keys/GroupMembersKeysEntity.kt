package app.paralino.core.db.main.keys

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
