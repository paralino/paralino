package app.paralino.core.ui.settings.billing

enum class BillingScreenSourceType(val key: String) {
    GROUP_ONBOARDING("group_onboarding"),
    SKIPPED_ONBOARDING("skipped_onboarding"),
    FREE_PLAN_NEW_USER("new_user_onboarding"),
    UPGRADE("notice"),
    FREE_PLAN_NOTICE("notice"),
    NORMAL("normal");

    val isOnboarding get() = when (this) {
        GROUP_ONBOARDING,
        SKIPPED_ONBOARDING,
        FREE_PLAN_NEW_USER -> true
        UPGRADE,
        FREE_PLAN_NOTICE,
        NORMAL -> false
    }

    val shouldPreselectPlan: Boolean get() = true

    companion object {
        fun fromKey(key: String?): BillingScreenSourceType {
            return BillingScreenSourceType.entries.firstOrNull { it.key == key } ?: NORMAL
        }
    }
}