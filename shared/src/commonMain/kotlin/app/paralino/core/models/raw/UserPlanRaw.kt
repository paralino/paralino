package app.paralino.core.models.raw

import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanType
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlin.time.Duration.Companion.minutes

class UserPlanRaw constructor(
    @SerialName("\$id")
    val id: String,
    val planType: String,
    val billingPeriod: PlanBillingPeriod,
    val groupId: String?,
    val ownerUserId: String?,
    val expiresAt: Instant?,
    val groupsLimitOverride: Long?,
    val provider: String?
) {

    // allow for some manual grace period
    // it might take few minutes until correct subscription data is synced and reflected on backend
    val isExpired: Boolean get() = expiresAt
        ?.plus(if (AppBuildConfig.instance.isDevMode) 1.minutes else 30.minutes)
        ?.let { it < nowUTC() }
        ?: false
    val planTypeEnum: PlanType = PlanType.fromKey(key = planType)

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
                expiresAt = (map["expiresAt"] as? String)?.isoWithOffsetToInstant(),
                groupsLimitOverride = (map["groupsLimitOverride"] as? Number?)?.toLong(),
                provider = map["provider"] as? String
            )
        }
    }
}