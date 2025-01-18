package app.paralino.core.ui.settings.billing

enum class BillingScreenSourceType(val key: String) {
    ONBOARDING("onboarding"),
    UPGRADE("notice"),
    FREE_PLAN_NOTICE("notice"),
    NORMAL("normal");

    val shouldPreselectPlan: Boolean get() = when (this) {
        ONBOARDING,
        UPGRADE -> true
        FREE_PLAN_NOTICE,
        NORMAL -> false
    }

    companion object {
        fun fromKey(key: String?): BillingScreenSourceType {
            return BillingScreenSourceType.entries.firstOrNull { it.key == key } ?: NORMAL
        }
    }
}