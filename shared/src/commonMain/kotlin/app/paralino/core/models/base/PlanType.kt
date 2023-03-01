package app.paralino.core.models.base

enum class PlanType(
    val key: String,
    val placesLimit: Int,
    val membersLimit: Int,
    val devicesLimit: Int,
) {
    BETA(
        key = "beta",
        placesLimit = 3,
        membersLimit = 10,
        devicesLimit = 2
    ),
    FREE(
        key = "free",
        placesLimit = 2,
        membersLimit = 5,
        devicesLimit = 1
    ),
    FAMILY(
        key = "family",
        placesLimit = 10,
        membersLimit = 20,
        devicesLimit = 3
    ),
    FAMILY_PLUS(
        key = "familyPlus",
        placesLimit = 1000,
        membersLimit = 50,
        devicesLimit = 10
    );

    companion object {
        val default get() = BETA
        val fallback get() = default
        fun fromKey(key: String): PlanType? {
            return values().firstOrNull { it.key == key }
        }
    }
}