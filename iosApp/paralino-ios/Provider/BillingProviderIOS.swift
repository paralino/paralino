//
//  BillingProviderIOS.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-03.
//

import Foundation
import RevenueCat
import Combine
import UIKit
import shared

class BillingProviderIOS: IBillingProvider {
    
    var isExternalProvider: Bool {
        return false
    }
    
    private lazy var appBuildConfig: AppBuildConfig = { KoinIOS.Instances.shared.appBuildConfig }()
    private lazy var userConfigRepo: UserConfigRepository = { KoinIOS.Instances.shared.userConfigRepo }()
    private lazy var remoteConfigRepo: RemoteConfigRepository = { KoinIOS.Instances.shared.remoteConfigRepo }()

    private var offerings = CurrentValueSubject<Offerings?, Never>(nil)
    private var currentUserPlan = CurrentValueSubject<CurrentPlanConfig?, Never>(nil)
    
    private func packageFor(subscriptionOptionId: String) -> Package? {
        return offerings.value?.current?.availablePackages.first(where: { package in
            package.storeProduct.productIdentifier == subscriptionOptionId
        })
    }
    
    private func packageFor(productId: String) -> Package? {
        return offerings.value?.current?.availablePackages.first(where: { package in
            package.storeProduct.productIdentifier == productId
        })
    }
    
    private func configure(billingUserId: String) throws -> Bool {
        Purchases.logLevel = appBuildConfig.logToConsole ? .debug : .error
        
        if Purchases.isConfigured, Purchases.shared.appUserID == billingUserId {
            return true
        }
        
        guard let billingRCApiKey = appBuildConfig.billingRCApiKey, billingRCApiKey.isEmpty == false else {
            let sharedError = PurchaseErrorException(
                errorCode: 0,
                errorMessage: "Missing RC billing key",
                userCancelled: false
            )
            throw PurchaseError(sharedError: sharedError)
        }
        
        Purchases.configure(withAPIKey: billingRCApiKey, appUserID: billingUserId)
        
        return false
    }
    
    func configure() async throws -> IBillingProviderSubscriptionProviderData {
        let billingUserId = try await userConfigRepo.ensureAndGetBillingUserId()
        let result = try configure(billingUserId: billingUserId)
        
        if !result {
            try await Task.sleep(nanoseconds: 800_000_000) // 800 milliseconds
        }
        
        try await loadPlatformHighestUserPlan()
        return try await loadAndGetProducts()
    }
    
    private func loadPlatformHighestUserPlan() async throws {
        let customerInfo = try await Purchases.shared.customerInfo(fetchPolicy: .fetchCurrent)
        let planConfigs = try await remoteConfigRepo.getConfigOnce().plans
        let activeEntitlements = customerInfo.entitlements.active.values

        let matchingPlans = activeEntitlements.compactMap { entitlementInfo -> CurrentPlanConfig? in
            let matchingConfig = planConfigs.first { config in
                config.productIds.contains(entitlementInfo.productIdentifier)
            }
            if let matchingConfig = matchingConfig, entitlementInfo.isActive {
                return CurrentPlanConfig(config: matchingConfig, entitlementInfo: entitlementInfo)
            } else {
                return nil
            }
        }

        let highestPlan = matchingPlans.max { $0.config.planTypeEnum.order < $1.config.planTypeEnum.order }
        currentUserPlan.value = highestPlan
    }
    
    private func loadAndGetProducts() async throws -> IBillingProviderSubscriptionProviderData {
        let offerings = try await Purchases.shared.offerings()
        self.offerings.value = offerings
        var products: [String: IBillingProviderSubscriptionPlanData] = [:]
        let billingConfig = try? await remoteConfigRepo.getBillingConfigFromRemote()

        if let offering = offerings.current {
            for package in offering.availablePackages {
                let product = package.storeProduct
                
                let billingPeriod: PlanBillingPeriod?
                if let subscriptionPeriod = product.subscriptionPeriod {
                    switch subscriptionPeriod.unit {
                    case .day:
                        billingPeriod = .onetime
                    case .week:
                        billingPeriod = .weekly
                    case .month:
                        billingPeriod = .monthly
                    case .year:
                        billingPeriod = .annually
                    default:
                        billingPeriod = nil
                    }
                } else {
                    billingPeriod = nil
                }

                if let billingPeriod = billingPeriod {
                    let price = product.priceDecimalNumber.int64Value
                    let formattedPrice: String?
                    switch billingPeriod {
                    case .weekly: formattedPrice = product.localizedPricePerWeek
                    case .monthly: formattedPrice =  product.localizedPricePerMonth
                    case .annually: formattedPrice = product.localizedPricePerYear
                    case .onetime: formattedPrice = product.localizedPriceString
                    default: formattedPrice = product.localizedPriceString
                    }
                    
                    let trialDurationDays: KotlinInt?
                    if await Purchases.shared.checkTrialOrIntroDiscountEligibility(product: product).isEligible {
                        trialDurationDays = product.introductoryDiscount?.subscriptionPeriod.valueInDays
                    } else {
                        trialDurationDays = nil
                    }
                    
                    let planConfig = try await remoteConfigRepo.getConfigOnce().plans.first { config in
                        config.productPlanIds.contains(product.productIdentifier)
                    }

                    if let planConfig = planConfig {
                        
                        let pricePerMember = product.formattedString(for: (product.pricePerMonth ?? 0).dividing(by: NSDecimalNumber(integerLiteral: Int(planConfig.membersLimit))))
                        
                        let periodData = IBillingProviderSubscriptionPlanPeriodData(
                            fullProductId: product.productIdentifier,
                            productPlanId: product.productIdentifier,
                            billingPeriod: billingPeriod,
                            price: price,
                            formattedPricePerMemberPerMonth: pricePerMember,
                            formattedPrice: formattedPrice ?? product.localizedPriceString,
                            referencePerYearPrice: KotlinLong(longLong: product.pricePerYear?.int64Value ?? 0),
                            referencePerYearFormattedPrice: product.localizedPricePerYear,
                            referencePerMonthFormattedPrice: product.localizedPricePerMonth,
                            trialDurationDays: trialDurationDays
                        )
                        
                        if let currentPlan = products[planConfig.planType] {
                            let newPlanData = IBillingProviderSubscriptionPlanData(
                                productId: currentPlan.productId,
                                periods: currentPlan.periods + [periodData],
                                planConfig: currentPlan.planConfig
                            )
                            products[planConfig.planType] = newPlanData
                        } else {
                            let newPlanData = IBillingProviderSubscriptionPlanData(
                                productId: planConfig.planType,
                                periods: [periodData],
                                planConfig: planConfig
                            )
                            products[planConfig.planType] = newPlanData
                        }
                    }
                }
            }
        }
        
        return IBillingProviderSubscriptionProviderData(
            plans: Array(products.values),
            billingConfig: billingConfig,
            canCloseOnboardingPaywall: offerings.current?.getMetadataValue(for: "canCloseOnboardingPaywall", default: true) ?? true
        )
    }

    
    func startPurchase(
        planType: PlanType,
        newProductPlanId: String
    ) async throws {
        guard let package = packageFor(subscriptionOptionId: newProductPlanId) else {
            throw PurchaseError(sharedError: PurchaseErrorException(errorCode: 0, errorMessage: "Package not found", userCancelled: false))
        }

        let presentedOffering: Package = package

        do {
            let result = try await Purchases.shared.purchase(package: presentedOffering)
            if result.userCancelled {
                let sharedError = PurchaseErrorException(
                    errorCode: Int32(ErrorCode.purchaseCancelledError.errorCode),
                    errorMessage: "User canceled",
                    userCancelled: true
                )
                throw PurchaseError(sharedError: sharedError)
            }
            // Handle successful purchase result
        } catch {
            if let error = error as? PurchaseError {
                throw error
            } else {
                let error = error as RevenueCat.PublicError
                let sharedError = PurchaseErrorException(
                    errorCode: Int32(error.code),
                    errorMessage: error.localizedDescription,
                    userCancelled: false
                )
                throw PurchaseError(sharedError: sharedError)
            }
        }
    }

    
    func restorePurchase() async throws {
        do {
            _ = try await Purchases.shared.restorePurchases()
        } catch let error as RevenueCat.PublicError {
            let sharedError = PurchaseErrorException(
                errorCode: Int32(error.code),
                errorMessage: error.localizedDescription,
                userCancelled: false
            )
            throw PurchaseError(sharedError: sharedError)
        }
    }
    
    func startDirectPurchase(url: String) async throws {
        // NO-OP
    }
    
    private struct CurrentPlanConfig {
        let config: PlanConfig
        let entitlementInfo: EntitlementInfo
    }
}

private extension StoreProduct {

    func formattedString(for price: NSDecimalNumber?) -> String? {
        guard let formatter = self.priceFormatter,
              let price = price
        else {
            return nil
        }

        return formatter.string(from: price as NSDecimalNumber)
    }

}

final class PurchaseError : Swift.Error, @unchecked Sendable {
    public let sharedError: PurchaseErrorException
    init(sharedError: PurchaseErrorException) {
        self.sharedError = sharedError
    }
}

extension SubscriptionPeriod {
    var valueInDays: KotlinInt? {
        switch unit {
        case .day:
            return KotlinInt(int: Int32(value))
        case .week:
            return KotlinInt(int: Int32(value * 7))
        case .month:
            return KotlinInt(int: Int32(value * 30))
        case .year:
            return KotlinInt(int: Int32(value * 365))
        @unknown default:
            return nil
        }
    }
}
