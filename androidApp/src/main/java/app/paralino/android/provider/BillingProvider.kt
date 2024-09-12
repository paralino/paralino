package app.paralino.android.provider

import app.paralino.core.models.AppBuildConfig
import app.paralino.core.provider.CoreAppClass
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.repo.user.UserConfigRepository
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import kotlinx.coroutines.delay

class BillingProvider(
    private val appClass: CoreAppClass,
    private val appBuildConfig: AppBuildConfig,
    private val userConfigRepo: UserConfigRepository
) : IBillingProvider {

    override suspend fun configure(waitIfNeeded: Boolean) {
        val billingUserId = userConfigRepo.ensureAndGetBillingUserId()
        val result = configure(billingUserId = billingUserId)
        if (!result && waitIfNeeded) {
            delay(800)
        }
    }

    override fun configure(billingUserId: String): Boolean {
        Purchases.logLevel = when (appBuildConfig.logToConsole) {
            true -> LogLevel.DEBUG
            false -> LogLevel.ERROR
        }

        if (Purchases.isConfigured && Purchases.sharedInstance.appUserID == billingUserId) {
            return true
        }

        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = appClass.applicationContext,
                apiKey = appBuildConfig.billingRCApiKey
            )
                .appUserID(appUserID = billingUserId)
                .build()
        )

        return false
    }
}