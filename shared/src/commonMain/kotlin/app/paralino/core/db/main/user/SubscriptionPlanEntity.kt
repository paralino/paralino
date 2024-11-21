package app.paralino.core.db.main.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptionPlanEntity")
data class SubscriptionPlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "planType") val planType: String,
    @ColumnInfo(name = "billingPeriod") val billingPeriod: String,
    @ColumnInfo(name = "groupId", defaultValue = "NULL") val groupId: String?,
    @ColumnInfo(name = "ownerUserId", defaultValue = "NULL") val ownerUserId: String?,
    @ColumnInfo(name = "expiresAt", defaultValue = "NULL") val expiresAt: String?,
    @ColumnInfo(name = "groupsLimitOverride", defaultValue = "NULL") val groupsLimitOverride: Long?,
    @ColumnInfo(name = "provider", defaultValue = "NULL") val provider: String?
)