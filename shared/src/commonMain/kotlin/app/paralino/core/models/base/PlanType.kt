package app.paralino.core.models.base

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

    data object PRO : PlanType() {
        override val key: String = "pro"
        override val order: Int = 100
    }

    data object MAX : PlanType() {
        override val key: String = "max"
        override val order: Int = 200
    }

    data class UNKNOWN(override val key: String, override val order: Int = -1) : PlanType()

    val isPremium: Boolean get() = order >= PRO.order
    val isFree: Boolean get() = this == FREE
    val isOtherThanSilver get() = this != SILVER

    companion object {
        fun fromKey(key: String): PlanType {
            return when (key) {
                FREE.key -> FREE
                SILVER.key -> SILVER
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
    val isActive: Boolean
) {

    val planTypeEnum: PlanType = PlanType.fromKey(key = planType)

    companion object {
        val default = PlanConfig(
            planType = PlanType.FREE.key,
            planName = PlanType.FREE.name,
            groupsLimit = 1,
            membersLimit = 5,
            devicesLimit = 1,
            placesLimit = 2,
            historyDaysLimit = 2,
            paralinoIdsLimit = 1,
            personalDevicesLimit = 2,
            isActive = true
        )
    }
}