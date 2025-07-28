package app.paralino.core.models.base

import app.paralino.core.MR
import app.paralino.core.provider.localizedString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PlanType.Serializer::class)
sealed class PlanType {
    abstract val key: String
    val name: String get() = key
    abstract val order: Int

    data object FREE : PlanType() {
        override val key: String = "free"
        override val order: Int = 0
    }

    data object SILVER : PlanType() {
        override val key: String = "silver"
        override val order: Int = 1
    }

    data object STANDARD : PlanType() {
        override val key: String = "standard"
        override val order: Int = 100
    }

    data object PRO : PlanType() {
        override val key: String = "pro"
        override val order: Int = 200
    }

    data object MAX : PlanType() {
        override val key: String = "max"
        override val order: Int = 300
    }

    data class UNKNOWN(override val key: String, override val order: Int = -1) : PlanType()

    val isSilverOrMore: Boolean get() = order >= SILVER.order
    val isPremium: Boolean get() = order >= STANDARD.order
    val isOtherThanSilver get() = this != SILVER

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + order
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        if (other !is PlanType) return false
        if (key != other.key) return false
        if (order != other.order) return false

        return true
    }

    companion object {
        fun fromKey(key: String): PlanType {
            return when (key) {
                FREE.key -> FREE
                SILVER.key -> SILVER
                STANDARD.key -> STANDARD
                PRO.key -> PRO
                MAX.key -> MAX
                else -> UNKNOWN(key)
            }
        }
    }

    object Serializer : KSerializer<PlanType> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("PlanType", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: PlanType) {
            encoder.encodeString(value.key)
        }

        override fun deserialize(decoder: Decoder): PlanType {
            val string = decoder.decodeString()
            return PlanType.fromKey(string)
        }
    }
}

enum class PlanBillingPeriod(val key: String) {
    WEEKLY(key = "weekly"),
    MONTHLY(key = "monthly"),
    ANNUALLY(key = "annually"),
    ONETIME(key = "oneTime");

    companion object {
        fun fromKey(key: String): PlanBillingPeriod? {
            return values().firstOrNull { it.key == key }
        }
    }
}

enum class PlanProvider(val key: String) {
    APPLE_APP_STORE(key = "app_store"),
    GOOGLE_PLAY_STORE(key = "play_store"),
    PARALINO(key = "paralino");

    val isPurchasedByUser: Boolean get() = this == APPLE_APP_STORE || this == GOOGLE_PLAY_STORE

    companion object {
        fun fromKey(key: String?): PlanProvider {
            return values().firstOrNull { it.key == key } ?: PARALINO
        }
    }
}

@Serializable
data class PlanConfig(
    val planType: String,
    val planName: String,
    val groupsLimit: Int,
    val membersLimit: Int,
    val devicesLimit: Int,
    val placesLimit: Int,
    val historyDaysLimit: Int,
    val paralinoIdsLimit: Int,
    val personalDevicesLimit: Int,
    val isActive: Boolean = true,
    val productIds: List<String> = emptyList(),
    val productPlanIds: List<String> = emptyList(),
) {

    val planTypeEnum: PlanType = PlanType.fromKey(key = planType)

    // TODO move to constructor when ready
    val personalHistoryDaysLimit: Int = historyDaysLimit

    val adjustedPlanName: String = when (planTypeEnum) {
        PlanType.SILVER -> MR.strings.paywall_silver_plan_name.localizedString()
        PlanType.FREE -> MR.strings.paywall_free_plan_name.localizedString()
        else -> planName
    }

    fun planBenefits(): List<PlanBenefit> {
        val planConfig = this
        return buildList {
            add(
                PlanBenefit(
                    content = MR.plurals.plan_benefit_group.localizedString(
                        planConfig.groupsLimit,
                        planConfig.groupsLimit
                    ),
                    isHighlighted = true
                )
            )
            add(
                PlanBenefit(
                    content = MR.plurals.plan_benefit_members.localizedString(
                        planConfig.membersLimit,
                        planConfig.membersLimit
                    ),
                    isHighlighted = true
                )
            )
            add(
                PlanBenefit(
                    content = MR.plurals.plan_benefit_places.localizedString(
                        planConfig.placesLimit,
                        planConfig.placesLimit
                    ),
                    isHighlighted = true,
                    separateLine = true
                )
            )
            if (planTypeEnum.isPremium) {
                add(
                    PlanBenefit(
                        content = "**${MR.strings.plans_benefit_location_requests.localizedString()}**",
                        isHighlighted = true,
                        separateLine = true
                    )
                )
            }
            add(
                PlanBenefit(
                    content = MR.strings.plans_benefit_sharing.localizedString(),
                    isHighlighted = true,
                    separateLine = true
                )
            )
        }
    }

    companion object {
        val default = PlanConfig(
            planType = PlanType.FREE.key,
            planName = PlanType.FREE.name,
            groupsLimit = 1,
            membersLimit = 2,
            devicesLimit = 1,
            placesLimit = 2,
            historyDaysLimit = 2,
            paralinoIdsLimit = 1,
            personalDevicesLimit = 1,
            isActive = true,
            productIds = listOf("free"),
            productPlanIds = listOf("free")
        )
    }

    data class PlanBenefit(
        val content: String,
        val isHighlighted: Boolean,
        val separateLine: Boolean = false
    )
}