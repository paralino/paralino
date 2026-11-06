package app.paralino.core.db.main.groups

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

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
    @ColumnInfo(name = "planExpiresAt") val planExpiresAt: String?,
    @ColumnInfo(name = "provider") val provider: String?,
    @ColumnInfo(name = "fallbackPlanType") val fallbackPlanType: String?,
    @ColumnInfo(name = "groupPlans") val groupPlans: String?
)
