package app.paralino.android.provider

import android.app.Activity
import app.paralino.core.models.base.PlanType
import app.paralino.core.provider.CoreAppClass
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserConfigRepository
import app.paralino.core.utils.extensions.openUri

class BillingProviderAndroid(
    private val appClass: CoreAppClass,
    private val appConfig: AppConfigRepository,
    private val userConfigRepo: UserConfigRepository,
    private val remoteConfigRepo: RemoteConfigRepository
) : IBillingProvider {

    override val isExternalProvider: Boolean = true

    override suspend fun configure(): IBillingProvider.SubscriptionProviderData {
        val billingUserId = userConfigRepo.ensureAndGetBillingUserId()
        return loadAndGetProducts()
    }

    private suspend fun loadAndGetProducts(): IBillingProvider.SubscriptionProviderData {

        val remoteBillingConfig = remoteConfigRepo.getBillingConfigFromRemote()
        val appConfig = remoteConfigRepo.getConfigOnce()
        val plans = appConfig.listedPlanConfigs()
            .filter { it.isPremium }
            .sortedBy { it.planOrder }

        val products = plans.map { plan ->
            IBillingProvider.SubscriptionPlanData(
                productId = plan.planType,
                periods = emptyList(),
                planConfig = plan
            )
        }

        return IBillingProvider.SubscriptionProviderData(
            plans = products,
            billingConfig = remoteBillingConfig,
            canCloseOnboardingPaywall = true,
            billingSelectorPosition = null
        )
    }

    suspend fun startPurchase(
        activity: Activity?,
        planType: PlanType,
        newProductPlanId: String
    ) {
        // NO-OP
    }

    override suspend fun startDirectPurchase(
        url: String
    ) {
        appClass.openUri(url)
    }

    suspend fun restorePurchase() {
        // NO-OP
    }
}