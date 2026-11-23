package app.paralino.core.models.raw

import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlinx.serialization.SerialName
import kotlin.time.Instant

data class UserPlanRaw constructor(
    @SerialName("\$id")
    val id: String,
    val planType: String,
    val billingPeriod: PlanBillingPeriod,
    val billingPeriodMultiplier: Int?,
    val groupId: String?,
    val ownerUserId: String?,
    val expiresAt: Instant?,
    val cancelledAt: Instant?,
    val updatedAt: Instant?,
    val groupsLimitOverride: Long?,
    val provider: PlanProvider
) {
    companion object {
        fun from(map: Map<String, Any>): UserPlanRaw {
            return UserPlanRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as? String,
                groupId = map["groupId"] as? String,
                planType = map["planType"] as String,
                billingPeriod = (map["billingPeriod"] as String).let {
                    PlanBillingPeriod.fromKey(it) ?: PlanBillingPeriod.ONETIME
                },
                billingPeriodMultiplier = (map["billingPeriodMultiplier"] as? Number)?.toInt(),
                expiresAt = (map["expiresAt"] as? String)?.isoWithOffsetToInstant(),
                cancelledAt = (map["cancelledAt"] as? String)?.isoWithOffsetToInstant(),
                updatedAt = (map["\$updatedAt"] as? String)?.isoWithOffsetToInstant(),
                groupsLimitOverride = (map["groupsLimitOverride"] as? Number?)?.toLong(),
                provider = PlanProvider.fromKey(map["provider"] as? String)
            )
        }
    }
}