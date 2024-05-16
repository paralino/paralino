package app.paralino.core.models.raw

import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanType
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserPlanRaw constructor(
    @SerialName("\$id")
    val id: String,
    val planType: PlanType,
    val billingPeriod: PlanBillingPeriod,
    val groupId: String?,
    val ownerUserId: String?,
    val expiresAt: Instant?,
    val groupsLimitOverride: Long?,
    val provider: String?
) {
    val isExpired: Boolean get() = expiresAt?.let { it < nowUTC() } ?: false
    companion object {
        fun from(map: Map<String, Any>): UserPlanRaw {
            return UserPlanRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as? String,
                groupId = map["groupId"] as? String,
                planType = (map["planType"] as String).let { PlanType.fromKey(it) ?: PlanType.default },
                billingPeriod = (map["billingPeriod"] as String).let { PlanBillingPeriod.fromKey(it) ?: PlanBillingPeriod.ONETIME },
                expiresAt = (map["expiresAt"] as? String)?.isoWithOffsetToInstant(),
                groupsLimitOverride = (map["groupsLimitOverride"] as? Number?)?.toLong(),
                provider = map["provider"] as? String
            )
        }
    }
}