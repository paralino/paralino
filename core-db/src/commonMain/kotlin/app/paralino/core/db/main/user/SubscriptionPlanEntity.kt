package app.paralino.core.db.main.user

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "subscriptionPlanEntity")
data class SubscriptionPlanEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "planType") val planType: String,
    @ColumnInfo(name = "billingPeriod") val billingPeriod: String,
    @ColumnInfo(name = "billingPeriodMultiplier", defaultValue = "NULL") val billingPeriodMultiplier: Int?,
    @ColumnInfo(name = "groupId", defaultValue = "NULL") val groupId: String?,
    @ColumnInfo(name = "ownerUserId", defaultValue = "NULL") val ownerUserId: String?,
    @ColumnInfo(name = "expiresAt", defaultValue = "NULL") val expiresAt: String?,
    @ColumnInfo(name = "cancelledAt", defaultValue = "NULL") val cancelledAt: String?,
    @ColumnInfo(name = "updatedAt", defaultValue = "NULL") val updatedAt: String?,
    @ColumnInfo(name = "groupsLimitOverride", defaultValue = "NULL") val groupsLimitOverride: Long?,
    @ColumnInfo(name = "provider", defaultValue = "NULL") val provider: String?
)