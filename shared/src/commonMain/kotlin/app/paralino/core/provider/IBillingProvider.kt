package app.paralino.core.provider

interface IBillingProvider {
    suspend fun configure(waitIfNeeded: Boolean)
    fun configure(billingUserId: String): Boolean
}