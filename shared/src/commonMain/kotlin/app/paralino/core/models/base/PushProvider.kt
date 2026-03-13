package app.paralino.core.models.base

enum class PushProvider(val key: String) {
    NOT_SET("not_set"),
    DEVICE_NATIVE("device_native"),
    PARALINO("paralino"),
    UNIFIED_PUSH("unified_push");

    companion object {
        val paralinoDistributorId = "app.paralino.android"
        val default = DEVICE_NATIVE
        fun fromKey(key: String?): PushProvider {
            return entries.firstOrNull { it.key == key } ?: default
        }
    }
}
