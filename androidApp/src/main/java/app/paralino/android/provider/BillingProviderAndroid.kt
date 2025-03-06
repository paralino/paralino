package app.paralino.android.provider

import android.app.Activity
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanType
import app.paralino.core.presentation.settings.billing.PurchaseErrorException
import app.paralino.core.provider.CoreAppClass
import app.paralino.core.provider.IBillingProvider
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.user.UserConfigRepository
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Period.Unit
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.googleProduct
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class BillingProviderAndroid(
    private val appClass: CoreAppClass,
    private val appBuildConfig: AppBuildConfig,
    private val userConfigRepo: UserConfigRepository,
    private val remoteConfigRepo: RemoteConfigRepository
) : IBillingProvider {

    private val offerings = MutableStateFlow<Offerings?>(null)
    private val currentUserPlan = MutableStateFlow<CurrentPlanConfig?>(null)

    private fun packageFor(subscriptionOptionId: String): Package? {
        return offerings.value?.current?.availablePackages?.firstOrNull {
            it.product.defaultOption?.id == subscriptionOptionId
        }
    }

    private fun packageFor(basePlanId: String, productId: String): Package? {
        return offerings.value?.current?.availablePackages?.firstOrNull {
            (it.product.googleProduct!!.defaultOption as? GoogleSubscriptionOption)?.let {
                it.basePlanId == basePlanId && it.productId == productId
            } ?: false
        }
    }

    private suspend fun loadPlatformHighestUserPlan() {
        Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
            .let { customer ->
                val planConfigs = remoteConfigRepo.getConfigOnce().plans
                customer.entitlements.active.mapNotNull { entitlement ->
                    val matchingConfig = planConfigs.firstOrNull {
                        it.productIds.any { it == entitlement.value.productIdentifier }
                    }
                    if (matchingConfig != null && entitlement.value.isActive) {
                        CurrentPlanConfig(matchingConfig, entitlement.value)
                    } else {
                        null
                    }
                }
            }
            .maxByOrNull { it.config.planTypeEnum.order }
            .also { currentUserPlan.value = it }
    }

    override suspend fun configure(): List<IBillingProvider.SubscriptionPlanData> {
        val billingUserId = userConfigRepo.ensureAndGetBillingUserId()
        val result = configure(billingUserId = billingUserId)

        // adding artificial delay since there is no callback in RC configure
        if (!result) {
            delay(800)
        }

        loadPlatformHighestUserPlan()
        return loadAndGetProducts()
    }

    private fun configure(billingUserId: String): Boolean {
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

    private suspend fun loadAndGetProducts(): List<IBillingProvider.SubscriptionPlanData> {
        val offerings = Purchases.sharedInstance.awaitOfferings()
        this.offerings.value = offerings
        val products = mutableMapOf<String, IBillingProvider.SubscriptionPlanData>()
        offerings.current?.let { offering ->
            offering.availablePackages.forEach { data ->

                val planConfig = remoteConfigRepo.getConfigOnce().plans.firstOrNull {
                    it.productIds.any { it == data.product.googleProduct!!.productId }
                }

                if (planConfig != null) {

                    val period = data.product.defaultOption?.let { subscriptionOption ->
                        val billingPeriod = when (subscriptionOption.billingPeriod?.unit) {
                            Period.Unit.UNKNOWN -> PlanBillingPeriod.ONETIME
                            Period.Unit.YEAR -> PlanBillingPeriod.ANNUALLY
                            Period.Unit.MONTH -> PlanBillingPeriod.MONTHLY
                            Period.Unit.WEEK -> PlanBillingPeriod.WEEKLY
                            else -> null
                        }
                        if (billingPeriod != null) {
                            IBillingProvider.SubscriptionPlanPeriodData(
                                fullProductId = data.product.id,
                                productPlanId = subscriptionOption.id,
                                billingPeriod = billingPeriod,
                                price = subscriptionOption.fullPricePhase?.price?.amountMicros ?: 0,
                                formattedPrice = when (billingPeriod) {
                                    PlanBillingPeriod.WEEKLY -> subscriptionOption.fullPricePhase
                                        ?.pricePerWeek()?.formatted ?: ""

                                    PlanBillingPeriod.MONTHLY -> subscriptionOption.fullPricePhase
                                        ?.pricePerMonth()?.formatted ?: ""

                                    PlanBillingPeriod.ANNUALLY -> subscriptionOption.fullPricePhase
                                        ?.pricePerYear()?.formatted ?: ""

                                    PlanBillingPeriod.ONETIME -> subscriptionOption.fullPricePhase
                                        ?.price?.formatted ?: ""
                                },
                                referencePerYearPrice = subscriptionOption.fullPricePhase
                                    ?.pricePerYear(Locale.getDefault())?.amountMicros ?: 0,
                                referencePerYearFormattedPrice = subscriptionOption.fullPricePhase
                                    ?.pricePerYear()?.formatted ?: "",
                                formattedPricePerMemberPerMonth = subscriptionOption.fullPricePhase?.pricePerMonth()?.pricePerPeriod(
                                    planConfig.membersLimit.toDouble()
                                )?.formatted,
                                referencePerMonthFormattedPrice = subscriptionOption.fullPricePhase?.pricePerMonth()?.formatted,
                                trialDurationDays = subscriptionOption.freePhase?.billingPeriod?.valueInDaysInt
                            )
                        } else {
                            null
                        }
                    }

                    val currentPlan = products[data.product.googleProduct!!.productId]

                    products[data.product.googleProduct!!.productId] = currentPlan?.copy(
                        periods = currentPlan.periods + listOfNotNull(period)
                    ) ?: IBillingProvider.SubscriptionPlanData(
                        productId = data.product.googleProduct!!.productId,
                        periods = listOfNotNull(period),
                        planConfig = planConfig
                    )
                }
            }
        }
        return products.values.toList()
    }

    suspend fun startPurchase(
        activity: Activity?,
        planType: PlanType,
        newProductPlanId: String
    ) {
        val packageInfo = packageFor(newProductPlanId)
        val platformPlan = currentUserPlan.value
        val oldPackageInfo = platformPlan?.let { plan ->
            packageFor(
                basePlanId = plan.entitlementInfo.productPlanIdentifier!!,
                productId = plan.entitlementInfo.productIdentifier
            )?.let { plan to it }
        }

        if (packageInfo != null && activity != null) {
            try {
                val params = PurchaseParams.Builder(activity, packageInfo)
                    .let {
                        if (oldPackageInfo == null) {
                            // no plan
                            it
                        } else if (planType.order > oldPackageInfo.first.config.planTypeEnum.order) {
                            // upgrading
                            if (oldPackageInfo.first.entitlementInfo.periodType == PeriodType.TRIAL) {
                                it.oldProductId(oldPackageInfo.second.product.id)
                                    .googleReplacementMode(GoogleReplacementMode.WITHOUT_PRORATION)
                            } else {
                                it.oldProductId(oldPackageInfo.second.product.id)
                                    .googleReplacementMode(GoogleReplacementMode.CHARGE_PRORATED_PRICE)
                            }
                        } else if (planType.order < oldPackageInfo.first.config.planTypeEnum.order) {
                            // downgrading
                            it.oldProductId(oldPackageInfo.second.product.id)
                                .googleReplacementMode(GoogleReplacementMode.WITH_TIME_PRORATION)
                        } else {
                            // same plan
                            it.oldProductId(oldPackageInfo.second.product.id)
                        }
                    }
                    .build()
                Purchases.sharedInstance.awaitPurchase(
                    purchaseParams = params
                )
            } catch (e: Exception) {
                if (e is PurchasesTransactionException) {
                    throw PurchaseErrorException(
                        errorCode = e.code.code,
                        errorMessage = e.error.message,
                        userCancelled = e.userCancelled
                    )
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun restorePurchase() {
        try {
            Purchases.sharedInstance.awaitRestore()
        } catch (e: Exception) {
            if (e is PurchasesTransactionException) {
                throw PurchaseErrorException(
                    errorCode = e.code.code,
                    errorMessage = e.error.message,
                    userCancelled = e.userCancelled
                )
            } else {
                throw e
            }
        }
    }

    private val Period.valueInDaysInt: Int?
        get() = when (unit) {
            Unit.DAY -> value
            Unit.WEEK -> value * 7
            Unit.MONTH -> value * 30
            Unit.YEAR -> value * 365
            Unit.UNKNOWN -> null
        }

    private val MICRO_MULTIPLIER = 1_000_000.0
    private fun Price.pricePerPeriod(units: Double, locale: Locale = Locale.getDefault()): Price {
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        numberFormat.currency = Currency.getInstance(currencyCode)

        val value = amountMicros / units
        val formatted = numberFormat.format(value / MICRO_MULTIPLIER)

        return Price(formatted, (value).toLong(), currencyCode)
    }


    private data class CurrentPlanConfig(
        val config: PlanConfig,
        val entitlementInfo: EntitlementInfo
    )
}