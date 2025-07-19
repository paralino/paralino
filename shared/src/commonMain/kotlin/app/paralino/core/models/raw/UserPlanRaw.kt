package app.paralino.core.models.raw

import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.base.PlanType
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UserPlanRaw constructor(
    @SerialName("\$id")
    val id: String,
    val planType: String,
    val billingPeriod: PlanBillingPeriod,
    val groupId: String?,
    val ownerUserId: String?,
    val expiresAt: Instant?,
    val groupsLimitOverride: Long?,
    val provider: PlanProvider
) {

    // allow for some manual grace period
    // it might take some time until correct subscription data is synced and reflected on backend
    // most of time it is just few minutes at most
    // In production we use multiple days as grace period if there are some billing issues
    val isExpired: Boolean get() = expiresAt
        ?.plus(if (AppBuildConfig.instance.isDevMode) 30.seconds else 2.minutes)
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
                provider = PlanProvider.fromKey(map["provider"] as? String)
            )
        }
    }
}