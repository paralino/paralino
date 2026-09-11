package app.paralino.core.db.main.groups

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groupEntity")
data class GroupEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "createdAt") val createdAt: String?,
    @ColumnInfo(name = "ownerUserId") val ownerUserId: String,
    @ColumnInfo(name = "groupKeyVersion") val groupKeyVersion: Long,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "serverIntegrity") val serverIntegrity: String,
    @ColumnInfo(name = "planType") val planType: String,
    @ColumnInfo(name = "schemeVersion") val schemeVersion: String,
    @ColumnInfo(name = "membersLimitOverride") val membersLimitOverride: Long?,
    @ColumnInfo(name = "devicesLimitOverride") val devicesLimitOverride: Long?,
    @ColumnInfo(name = "placesLimitOverride") val placesLimitOverride: Long?,
    @ColumnInfo(name = "planExpiresAt") val planExpiresAt: String?,
    @ColumnInfo(name = "provider") val provider: String?,
    @ColumnInfo(name = "fallbackPlanType") val fallbackPlanType: String?
)
