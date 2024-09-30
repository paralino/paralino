package app.paralino.core.ui.settings.billing

enum class BillingScreenSourceType(val key: String) {
    ONBOARDING("onboarding"),
    FREE_PLAN_NOTICE("notice"),
    NORMAL("normal");

    companion object {
        fun fromKey(key: String?): BillingScreenSourceType {
            return BillingScreenSourceType.entries.firstOrNull { it.key == key } ?: NORMAL
        }
    }
}