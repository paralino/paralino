package app.paralino.core.presentation.billing

enum class BillingScreenSourceType(val key: String) {
    GROUP_ONBOARDING("group_onboarding"),
    PERSONAL_SPACE_ONBOARDING("personalSpace_onboarding"),
    SKIPPED_ONBOARDING("skipped_onboarding"),
    FREE_PLAN_NEW_GROUP_MEMBER("new_group_member_added"),
    UPGRADE_MAP_SHEET("upgrade_map_sheet"),
    UPGRADE_BILLING_PROMO("upgrade_billing_promo"),
    UPGRADE("upgrade_regular"),
    NORMAL("normal");

    val isOnboarding get() = when (this) {
        GROUP_ONBOARDING,
        PERSONAL_SPACE_ONBOARDING,
        SKIPPED_ONBOARDING,
        FREE_PLAN_NEW_GROUP_MEMBER -> true

        UPGRADE_MAP_SHEET,
        UPGRADE_BILLING_PROMO,
        UPGRADE,
        NORMAL -> false
    }

    val shouldPreselectPlan: Boolean get() = true

    companion object {
        fun fromKey(key: String?): BillingScreenSourceType {
            return entries.firstOrNull { it.key == key } ?: NORMAL
        }
    }
}