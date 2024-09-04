package app.paralino.android.provider

import app.paralino.core.models.AppBuildConfig
import app.paralino.core.provider.CoreAppClass
import app.paralino.core.provider.IBillingProvider
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class BillingProvider(
    private val appClass: CoreAppClass,
    private val appBuildConfig: AppBuildConfig
): IBillingProvider {
    override fun configure(billingUserId: String) {
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(
                context = appClass.applicationContext,
                apiKey = appBuildConfig.billingRCApiKey
            )
                .appUserID(appUserID = billingUserId)
                .build()
        )
    }
}